package com.loantracker.backend.service;

import com.loantracker.backend.dto.LoanEntryDto;
import com.loantracker.backend.dto.InstallmentPlanDto;
import com.loantracker.backend.dto.PaymentAllocationDto;
import com.loantracker.backend.entity.GroupEntity;
import com.loantracker.backend.entity.LoanEntry;
import com.loantracker.backend.entity.Person;
import com.loantracker.backend.repository.GroupEntityRepository;
import com.loantracker.backend.repository.LoanEntryRepository;
import com.loantracker.backend.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LoanEntryService {

    private final LoanEntryRepository loanEntryRepository;
    private final PersonRepository personRepository;
    private final GroupEntityRepository groupEntityRepository;
    private final InstallmentPlanService installmentPlanService;
    private final PaymentAllocationService paymentAllocationService;

    @Transactional(readOnly = true)
    public List<LoanEntryDto> getAllLoans() {
        return loanEntryRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoanEntryDto getLoanById(UUID entryId) {
        LoanEntry entry = loanEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Loan entry not found with ID: " + entryId));
        return convertToDto(entry);
    }

    @Transactional(readOnly = true)
    public List<LoanEntryDto> getLoansByPersonId(UUID personId) {
        return loanEntryRepository.findByPersonId(personId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanEntryDto> getLoansByGroupId(UUID groupId) {
        return loanEntryRepository.findByGroupId(groupId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanEntryDto createLoan(LoanEntryDto dto) {
        LoanEntry entry = new LoanEntry();
        updateEntryFromDto(entry, dto);
        
        // Generate Reference ID
        entry.setReferenceId(generateReferenceId(entry));

        // Default fields
        if (entry.getAmountRemaining() == null) {
            entry.setAmountRemaining(entry.getAmountBorrowed());
        }
        if (entry.getPaymentStatus() == null || entry.getPaymentStatus().isEmpty()) {
            entry.setPaymentStatus("UNPAID");
        }

        LoanEntry saved = loanEntryRepository.save(entry);

        // Save Installment Plan if transaction type is Installment Expense
        if ("Installment Expense".equalsIgnoreCase(saved.getTransactionType()) && dto.getInstallmentPlan() != null) {
            InstallmentPlanDto planDto = dto.getInstallmentPlan();
            planDto.setEntryId(saved.getEntryId());
            installmentPlanService.savePlan(planDto);
        }

        // Save Payment Allocations if transaction type is Group Expense
        if ("Group Expense".equalsIgnoreCase(saved.getTransactionType()) && dto.getPaymentAllocations() != null) {
            paymentAllocationService.saveAllocations(
                    saved.getEntryId(),
                    dto.getPaymentAllocations(),
                    dto.getSplitType() != null ? dto.getSplitType() : "EQUAL",
                    saved.getAmountBorrowed()
            );
        }

        return convertToDto(saved);
    }

    @Transactional
    public LoanEntryDto updateLoan(UUID entryId, LoanEntryDto dto) {
        LoanEntry entry = loanEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Loan entry not found with ID: " + entryId));

        updateEntryFromDto(entry, dto);

        // Regenerate Reference ID in case borrower/lender changed
        entry.setReferenceId(generateReferenceId(entry));

        LoanEntry saved = loanEntryRepository.save(entry);

        // Update/Save/Delete Installment Plan
        if ("Installment Expense".equalsIgnoreCase(saved.getTransactionType()) && dto.getInstallmentPlan() != null) {
            InstallmentPlanDto planDto = dto.getInstallmentPlan();
            planDto.setEntryId(saved.getEntryId());
            installmentPlanService.savePlan(planDto);
        } else if (!"Installment Expense".equalsIgnoreCase(saved.getTransactionType())) {
            installmentPlanService.deletePlanByEntryId(saved.getEntryId());
        }

        // Update/Save/Delete Payment Allocations
        if ("Group Expense".equalsIgnoreCase(saved.getTransactionType()) && dto.getPaymentAllocations() != null) {
            paymentAllocationService.saveAllocations(
                    saved.getEntryId(),
                    dto.getPaymentAllocations(),
                    dto.getSplitType() != null ? dto.getSplitType() : "EQUAL",
                    saved.getAmountBorrowed()
            );
        } else if (!"Group Expense".equalsIgnoreCase(saved.getTransactionType())) {
            paymentAllocationService.saveAllocations(saved.getEntryId(), new java.util.ArrayList<>(), "EQUAL", 0.0);
        }

        return convertToDto(saved);
    }

    @Transactional
    public void deleteLoan(UUID entryId) {
        LoanEntry entry = loanEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Loan entry not found with ID: " + entryId));
        loanEntryRepository.delete(entry);
    }

    private void updateEntryFromDto(LoanEntry entry, LoanEntryDto dto) {
        entry.setEntryName(dto.getEntryName());
        entry.setDescription(dto.getDescription());
        entry.setTransactionType(dto.getTransactionType());
        entry.setDateBorrowed(dto.getDateBorrowed());
        entry.setDateFullyPaid(dto.getDateFullyPaid());
        entry.setAmountBorrowed(dto.getAmountBorrowed());
        entry.setSplitType(dto.getSplitType());
        
        if (dto.getAmountRemaining() != null) {
            entry.setAmountRemaining(dto.getAmountRemaining());
        } else {
            entry.setAmountRemaining(dto.getAmountBorrowed());
        }

        // Auto-complete logic (mark entry as fully paid, partially paid, or unpaid based on amountRemaining)
        if (entry.getAmountRemaining() != null) {
            double remaining = entry.getAmountRemaining();
            if (Math.round(remaining) <= 0) {
                entry.setAmountRemaining(0.0);
                entry.setPaymentStatus("PAID");
                if (entry.getDateFullyPaid() == null) {
                    entry.setDateFullyPaid(new java.util.Date());
                }
            } else if (remaining < entry.getAmountBorrowed()) {
                entry.setPaymentStatus("PARTIALLY PAID");
                entry.setDateFullyPaid(null);
            } else {
                entry.setPaymentStatus("UNPAID");
                entry.setDateFullyPaid(null);
            }
        } else {
            entry.setPaymentStatus("UNPAID");
            entry.setDateFullyPaid(null);
        }

        entry.setNotes(dto.getNotes());
        entry.setPaymentNotes(dto.getPaymentNotes());

        // Resolve Borrower Person or Group by Name/ID
        if (dto.getBorrowerPersonName() != null && !dto.getBorrowerPersonName().trim().isEmpty()) {
            Person borrower = findOrCreatePerson(dto.getBorrowerPersonName());
            entry.setBorrowerPerson(borrower);
            entry.setBorrowerGroup(null); // Mutual exclusivity
        } else if (dto.getBorrowerPersonId() != null) {
            Person borrower = personRepository.findById(dto.getBorrowerPersonId())
                    .orElseThrow(() -> new IllegalArgumentException("Borrower Person not found with ID: " + dto.getBorrowerPersonId()));
            entry.setBorrowerPerson(borrower);
            entry.setBorrowerGroup(null); // Mutual exclusivity
        } else if (dto.getBorrowerGroupName() != null && !dto.getBorrowerGroupName().trim().isEmpty()) {
            GroupEntity group = findOrCreateGroup(dto.getBorrowerGroupName());
            entry.setBorrowerGroup(group);
            entry.setBorrowerPerson(null); // Mutual exclusivity
        } else if (dto.getBorrowerGroupId() != null) {
            GroupEntity group = groupEntityRepository.findById(dto.getBorrowerGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Borrower Group not found with ID: " + dto.getBorrowerGroupId()));
            entry.setBorrowerGroup(group);
            entry.setBorrowerPerson(null); // Mutual exclusivity
        } else {
            entry.setBorrowerPerson(null);
            entry.setBorrowerGroup(null);
        }

        // Resolve Lender Person Name/ID
        if (dto.getLenderPersonName() != null && !dto.getLenderPersonName().trim().isEmpty()) {
            Person lender = findOrCreatePerson(dto.getLenderPersonName());
            entry.setLenderPerson(lender);
        } else if (dto.getLenderPersonId() != null) {
            Person lender = personRepository.findById(dto.getLenderPersonId())
                    .orElseThrow(() -> new IllegalArgumentException("Lender Person not found with ID: " + dto.getLenderPersonId()));
            entry.setLenderPerson(lender);
        } else {
            entry.setLenderPerson(null);
        }

        // Decode receipt proof if present
        if (dto.getReceiptProofBase64() != null && !dto.getReceiptProofBase64().trim().isEmpty()) {
            String base64Data = dto.getReceiptProofBase64();
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            try {
                entry.setReceiptProof(Base64.getDecoder().decode(base64Data));
            } catch (Exception e) {
                // Ignore decoding errors or set default null
                entry.setReceiptProof(null);
            }
        }
    }

    private LoanEntryDto convertToDto(LoanEntry entry) {
        String base64Proof = null;
        if (entry.getReceiptProof() != null) {
            base64Proof = "data:image/png;base64," + Base64.getEncoder().encodeToString(entry.getReceiptProof());
        }

        InstallmentPlanDto planDto = null;
        if ("Installment Expense".equalsIgnoreCase(entry.getTransactionType())) {
            planDto = installmentPlanService.getPlanByEntryId(entry.getEntryId());
        }

        List<PaymentAllocationDto> allocationDtos = null;
        if ("Group Expense".equalsIgnoreCase(entry.getTransactionType())) {
            allocationDtos = paymentAllocationService.getAllocationsByEntryId(entry.getEntryId());
        }

        return LoanEntryDto.builder()
                .entryId(entry.getEntryId())
                .referenceId(entry.getReferenceId())
                .entryName(entry.getEntryName())
                .description(entry.getDescription())
                .transactionType(entry.getTransactionType())
                .dateBorrowed(entry.getDateBorrowed())
                .dateFullyPaid(entry.getDateFullyPaid())
                .borrowerPersonId(entry.getBorrowerPerson() != null ? entry.getBorrowerPerson().getPersonId() : null)
                .borrowerPersonName(entry.getBorrowerPerson() != null ? 
                        entry.getBorrowerPerson().getFirstName() + " " + entry.getBorrowerPerson().getLastName() : null)
                .borrowerGroupId(entry.getBorrowerGroup() != null ? entry.getBorrowerGroup().getGroupId() : null)
                .borrowerGroupName(entry.getBorrowerGroup() != null ? entry.getBorrowerGroup().getGroupName() : null)
                .lenderPersonId(entry.getLenderPerson() != null ? entry.getLenderPerson().getPersonId() : null)
                .lenderPersonName(entry.getLenderPerson() != null ? 
                        entry.getLenderPerson().getFirstName() + " " + entry.getLenderPerson().getLastName() : null)
                .amountBorrowed(entry.getAmountBorrowed())
                .amountRemaining(entry.getAmountRemaining())
                .paymentStatus(entry.getPaymentStatus())
                .notes(entry.getNotes())
                .paymentNotes(entry.getPaymentNotes())
                .receiptProofBase64(base64Proof)
                .installmentPlan(planDto)
                .paymentAllocations(allocationDtos)
                .splitType(entry.getSplitType())
                .build();
    }

    private String generateReferenceId(LoanEntry entry) {
        String borrowerInitials = "UNK";
        if (entry.getBorrowerPerson() != null) {
            borrowerInitials = getPersonInitials(entry.getBorrowerPerson());
        } else if (entry.getBorrowerGroup() != null) {
            borrowerInitials = getGroupInitials(entry.getBorrowerGroup());
        }

        String lenderInitials = "UNK";
        if (entry.getLenderPerson() != null) {
            lenderInitials = getPersonInitials(entry.getLenderPerson());
        }

        return borrowerInitials + "-" + lenderInitials;
    }

    private String getPersonInitials(Person person) {
        if (person.getInitials() != null && !person.getInitials().trim().isEmpty()) {
            return person.getInitials().trim().toUpperCase();
        }
        
        String fullName = (person.getFirstName() + " " + person.getLastName()).trim();
        return getInitialsFromString(fullName);
    }

    private String getGroupInitials(GroupEntity group) {
        String groupName = group.getGroupName().trim();
        String[] words = groupName.split("\\s+");
        if (words.length > 1) {
            return getInitialsFromString(groupName);
        } else {
            // If single word, take first 3 letters uppercase
            if (groupName.length() >= 3) {
                return groupName.substring(0, 3).toUpperCase();
            } else {
                return groupName.toUpperCase();
            }
        }
    }

    private String getInitialsFromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "UNK";
        }
        String[] words = text.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return sb.toString();
    }

    private Person findOrCreatePerson(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return null;
        }
        final String trimmedFullName = fullName.trim();
        int lastSpaceIdx = trimmedFullName.lastIndexOf(' ');
        final String firstName;
        final String lastName;
        if (lastSpaceIdx == -1) {
            firstName = trimmedFullName;
            lastName = "";
        } else {
            firstName = trimmedFullName.substring(0, lastSpaceIdx).trim();
            lastName = trimmedFullName.substring(lastSpaceIdx + 1).trim();
        }

        return personRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName)
                .orElseGet(() -> {
                    String initials = getInitialsFromString(trimmedFullName);
                    Person newPerson = Person.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .initials(initials)
                            .build();
                    return personRepository.save(newPerson);
                });
    }

    private GroupEntity findOrCreateGroup(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return null;
        }
        groupName = groupName.trim();
        final String nameToFind = groupName;
        return groupEntityRepository.findByGroupNameIgnoreCase(nameToFind)
                .orElseGet(() -> {
                    GroupEntity newGroup = GroupEntity.builder()
                            .groupName(nameToFind)
                            .build();
                    return groupEntityRepository.save(newGroup);
                });
    }
}

