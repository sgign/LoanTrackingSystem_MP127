package com.loantracker.backend.service;

import com.loantracker.backend.dto.PaymentAllocationDto;
import com.loantracker.backend.entity.LoanEntry;
import com.loantracker.backend.entity.PaymentAllocation;
import com.loantracker.backend.entity.Person;
import com.loantracker.backend.entity.Payment;
import com.loantracker.backend.repository.LoanEntryRepository;
import com.loantracker.backend.repository.PaymentAllocationRepository;
import com.loantracker.backend.repository.PaymentRepository;
import com.loantracker.backend.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentAllocationService {

    private final PaymentAllocationRepository paymentAllocationRepository;
    private final LoanEntryRepository loanEntryRepository;
    private final PersonRepository personRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PaymentAllocationDto> getAllocationsByEntryId(UUID entryId) {
        return paymentAllocationRepository.findByLoanEntry_EntryId(entryId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAllocationsByEntryId(UUID entryId) {
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByLoanEntry_EntryId(entryId);
        if (!allocations.isEmpty()) {
            paymentAllocationRepository.deleteAll(allocations);
        }
    }

    @Transactional
    public List<PaymentAllocationDto> saveAllocations(UUID entryId, List<PaymentAllocationDto> dtos, String splitType, Double totalAmount) {
        LoanEntry entry = loanEntryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Loan entry not found with ID: " + entryId));

        // Delete existing allocations first to replace them
        List<PaymentAllocation> existing = paymentAllocationRepository.findByLoanEntry_EntryId(entryId);
        paymentAllocationRepository.deleteAll(existing);

        if (dtos == null || dtos.isEmpty()) {
            return new ArrayList<>();
        }

        // Perform calculation/splitting
        List<PaymentAllocationDto> calculated = calculateSplit(totalAmount, dtos, splitType);

        List<PaymentAllocation> toSave = new ArrayList<>();
        for (PaymentAllocationDto calcDto : calculated) {
            Person payee = null;
            if (calcDto.getPayeePersonId() != null) {
                payee = personRepository.findById(calcDto.getPayeePersonId()).orElse(null);
            }
            if (payee == null && calcDto.getPayeePersonName() != null && !calcDto.getPayeePersonName().trim().isEmpty()) {
                payee = findOrCreatePerson(calcDto.getPayeePersonName());
            }

            if (payee == null) {
                throw new IllegalArgumentException("Could not resolve payee person for allocation: " + calcDto.getPayeePersonName());
            }

            PaymentAllocation alloc = PaymentAllocation.builder()
                    .loanEntry(entry)
                    .description(calcDto.getDescription())
                    .payeePerson(payee)
                    .amount(calcDto.getAmount())
                    .percentageOfTotal(calcDto.getPercentageOfTotal())
                    .status(calcDto.getStatus() != null ? calcDto.getStatus() : "UNPAID")
                    .notes(calcDto.getNotes())
                    .build();

            toSave.add(alloc);
        }

        List<PaymentAllocation> saved = paymentAllocationRepository.saveAll(toSave);

        // Synchronize Payment records based on allocation statuses
        List<Payment> existingPayments = paymentRepository.findByLoanEntry_EntryId(entryId);
        for (PaymentAllocation alloc : saved) {
            Person payee = alloc.getPayeePerson();
            if (payee == null) continue;

            boolean hasPayment = false;
            Payment matchingPayment = null;
            for (Payment p : existingPayments) {
                if (p.getPayeePerson() != null && p.getPayeePerson().getPersonId().equals(payee.getPersonId())) {
                    hasPayment = true;
                    matchingPayment = p;
                    break;
                }
            }

            if ("PAID".equals(alloc.getStatus())) {
                if (!hasPayment) {
                    Payment newPayment = Payment.builder()
                            .loanEntry(entry)
                            .payeePerson(payee)
                            .paymentAmount(alloc.getAmount())
                            .paymentDate(new java.util.Date())
                            .notes("Auto-generated from Group Split Allocation")
                            .build();
                    paymentRepository.save(newPayment);
                }
            } else if ("UNPAID".equals(alloc.getStatus())) {
                if (hasPayment && matchingPayment != null) {
                    paymentRepository.delete(matchingPayment);
                }
            }
        }

        // Recalculate loan entry amountRemaining and status
        List<Payment> finalPayments = paymentRepository.findByLoanEntry_EntryId(entryId);
        double totalPaid = finalPayments.stream()
                .mapToDouble(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : 0.0)
                .sum();
        double amountBorrowed = entry.getAmountBorrowed() != null ? entry.getAmountBorrowed() : 0.0;
        double newRemaining = Math.max(0.0, amountBorrowed - totalPaid);

        if (Math.round(newRemaining) <= 0) {
            entry.setAmountRemaining(0.0);
            entry.setPaymentStatus("PAID");
            if (entry.getDateFullyPaid() == null) {
                entry.setDateFullyPaid(new java.util.Date());
            }
        } else {
            entry.setAmountRemaining(newRemaining);
            if (newRemaining < amountBorrowed) {
                entry.setPaymentStatus("PARTIALLY PAID");
                entry.setDateFullyPaid(null);
            } else {
                entry.setPaymentStatus("UNPAID");
                entry.setDateFullyPaid(null);
            }
        }
        loanEntryRepository.save(entry);

        return saved.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<PaymentAllocationDto> calculateSplit(Double totalAmount, List<PaymentAllocationDto> inputs, String splitType) {
        if (inputs == null || inputs.isEmpty()) {
            return new ArrayList<>();
        }

        int count = inputs.size();
        String type = splitType != null ? splitType.toUpperCase() : "EQUAL";
        List<PaymentAllocationDto> results = new ArrayList<>();

        switch (type) {
            case "PERCENT":
                double totalPct = 0;
                for (PaymentAllocationDto input : inputs) {
                    double pct = input.getPercentageOfTotal() != null ? input.getPercentageOfTotal() : 0.0;
                    totalPct += pct;
                }
                // Allow minor floating point inaccuracy (e.g. 99.9% to 100.1%)
                if (Math.abs(totalPct - 100.0) > 0.1) {
                    throw new IllegalArgumentException("Sum of percentages must equal 100. Total was: " + totalPct);
                }

                for (PaymentAllocationDto input : inputs) {
                    double pct = input.getPercentageOfTotal() != null ? input.getPercentageOfTotal() : 0.0;
                    double amt = (totalAmount * pct) / 100.0;
                    results.add(PaymentAllocationDto.builder()
                            .payeePersonId(input.getPayeePersonId())
                            .payeePersonName(input.getPayeePersonName())
                            .description(input.getDescription())
                            .amount(Math.round(amt * 100.0) / 100.0)
                            .percentageOfTotal(pct)
                            .status(input.getStatus())
                            .notes(input.getNotes())
                            .build());
                }
                break;

            case "AMOUNT":
                double totalAmt = 0;
                for (PaymentAllocationDto input : inputs) {
                    double amt = input.getAmount() != null ? input.getAmount() : 0.0;
                    totalAmt += amt;
                }
                if (Math.abs(totalAmt - totalAmount) > 0.05) {
                    throw new IllegalArgumentException(String.format("Sum of allocated amounts ($%.2f) must equal total loan amount ($%.2f)", totalAmt, totalAmount));
                }

                for (PaymentAllocationDto input : inputs) {
                    double amt = input.getAmount() != null ? input.getAmount() : 0.0;
                    double pct = (amt / totalAmount) * 100.0;
                    results.add(PaymentAllocationDto.builder()
                            .payeePersonId(input.getPayeePersonId())
                            .payeePersonName(input.getPayeePersonName())
                            .description(input.getDescription())
                            .amount(amt)
                            .percentageOfTotal(Math.round(pct * 100.0) / 100.0)
                            .status(input.getStatus())
                            .notes(input.getNotes())
                            .build());
                }
                break;

            case "EQUAL":
            default:
                double equalAmt = totalAmount / count;
                double equalPct = 100.0 / count;
                for (PaymentAllocationDto input : inputs) {
                    results.add(PaymentAllocationDto.builder()
                            .payeePersonId(input.getPayeePersonId())
                            .payeePersonName(input.getPayeePersonName())
                            .description(input.getDescription())
                            .amount(Math.round(equalAmt * 100.0) / 100.0)
                            .percentageOfTotal(Math.round(equalPct * 100.0) / 100.0)
                            .status(input.getStatus())
                            .notes(input.getNotes())
                            .build());
                }
                break;
        }

        return results;
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

    private PaymentAllocationDto convertToDto(PaymentAllocation alloc) {
        return PaymentAllocationDto.builder()
                .allocationId(alloc.getAllocationId())
                .entryId(alloc.getLoanEntry().getEntryId())
                .description(alloc.getDescription())
                .payeePersonId(alloc.getPayeePerson() != null ? alloc.getPayeePerson().getPersonId() : null)
                .payeePersonName(alloc.getPayeePerson() != null ? 
                        alloc.getPayeePerson().getFirstName() + " " + alloc.getPayeePerson().getLastName() : null)
                .amount(alloc.getAmount())
                .percentageOfTotal(alloc.getPercentageOfTotal())
                .status(alloc.getStatus())
                .notes(alloc.getNotes())
                .build();
    }

    @Transactional
    public void recalculateAllocationStatuses(UUID entryId) {
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByLoanEntry_EntryId(entryId);
        if (allocations.isEmpty()) {
            return;
        }

        List<Payment> payments = paymentRepository.findByLoanEntry_EntryId(entryId);
        
        // Group payments by payeePersonId
        Map<UUID, Double> paidAmounts = new HashMap<>();
        for (Payment p : payments) {
            if (p.getPayeePerson() != null) {
                UUID personId = p.getPayeePerson().getPersonId();
                double amt = p.getPaymentAmount() != null ? p.getPaymentAmount() : 0.0;
                paidAmounts.put(personId, paidAmounts.getOrDefault(personId, 0.0) + amt);
            }
        }
        
        for (PaymentAllocation alloc : allocations) {
            if (alloc.getPayeePerson() == null) continue;
            UUID personId = alloc.getPayeePerson().getPersonId();
            double totalPaid = paidAmounts.getOrDefault(personId, 0.0);
            double requiredAmt = alloc.getAmount() != null ? alloc.getAmount() : 0.0;
            
            if (totalPaid >= requiredAmt && requiredAmt > 0) {
                alloc.setStatus("PAID");
            } else if (totalPaid > 0) {
                alloc.setStatus("PARTIALLY PAID");
            } else {
                alloc.setStatus("UNPAID");
            }
        }
        
        paymentAllocationRepository.saveAll(allocations);
    }
}
