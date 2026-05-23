package com.loantracker.backend.service;

import com.loantracker.backend.dto.InstallmentPlanDto;
import com.loantracker.backend.dto.InstallmentTermDto;
import com.loantracker.backend.entity.InstallmentPlan;
import com.loantracker.backend.entity.InstallmentTerm;
import com.loantracker.backend.entity.InstallmentTermStatus;
import com.loantracker.backend.entity.LoanEntry;
import com.loantracker.backend.entity.Payment;
import com.loantracker.backend.repository.InstallmentPlanRepository;
import com.loantracker.backend.repository.InstallmentTermRepository;
import com.loantracker.backend.repository.LoanEntryRepository;
import com.loantracker.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InstallmentPlanService {

    private final InstallmentPlanRepository installmentPlanRepository;
    private final InstallmentTermRepository installmentTermRepository;
    private final LoanEntryRepository loanEntryRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public InstallmentPlanDto getPlanByEntryId(UUID entryId) {
        Optional<InstallmentPlan> planOpt = installmentPlanRepository.findByLoanEntry_EntryId(entryId);
        if (planOpt.isEmpty()) {
            return null;
        }
        InstallmentPlan plan = planOpt.get();
        recalculateTermStatuses(entryId);
        List<InstallmentTerm> terms = installmentTermRepository.findByInstallmentPlan_InstallmentIdOrderByTermNumberAsc(plan.getInstallmentId());
        return convertToDto(plan, terms);
    }

    @Transactional
    public InstallmentPlanDto savePlan(InstallmentPlanDto dto) {
        LoanEntry entry = loanEntryRepository.findById(dto.getEntryId())
                .orElseThrow(() -> new IllegalArgumentException("Loan entry not found with ID: " + dto.getEntryId()));

        Optional<InstallmentPlan> existingOpt = installmentPlanRepository.findByLoanEntry_EntryId(dto.getEntryId());
        InstallmentPlan plan;
        boolean isNew = false;

        if (existingOpt.isPresent()) {
            plan = existingOpt.get();
        } else {
            plan = new InstallmentPlan();
            plan.setLoanEntry(entry);
            isNew = true;
        }

        plan.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : new Date());
        plan.setPaymentFrequency(dto.getPaymentFrequency() != null ? dto.getPaymentFrequency() : "MONTHLY");
        plan.setPaymentTerms(dto.getPaymentTerms() != null ? dto.getPaymentTerms() : 1);
        plan.setAmountPerTerm(dto.getAmountPerTerm() != null ? dto.getAmountPerTerm() : entry.getAmountBorrowed() / plan.getPaymentTerms());
        plan.setPaymentDay(dto.getPaymentDay());
        plan.setNotes(dto.getNotes());
        plan.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        if ("MONTHLY".equalsIgnoreCase(plan.getPaymentFrequency())) {
            if (plan.getPaymentDay() != null && (plan.getPaymentDay() < 1 || plan.getPaymentDay() > 28)) {
                throw new IllegalArgumentException("For MONTHLY frequency, payment day must be between 1 and 28.");
            }
        } else if ("WEEKLY".equalsIgnoreCase(plan.getPaymentFrequency())) {
            if (plan.getPaymentDay() != null && (plan.getPaymentDay() < 1 || plan.getPaymentDay() > 7)) {
                throw new IllegalArgumentException("For WEEKLY frequency, payment day must be between 1 (Sunday) and 7 (Saturday).");
            }
        }

        InstallmentPlan savedPlan = installmentPlanRepository.save(plan);

        List<InstallmentTerm> terms;
        if (isNew) {
            terms = generateTerms(savedPlan);
        } else {
            // If terms count changed, regenerate them
            List<InstallmentTerm> existingTerms = installmentTermRepository.findByInstallmentPlan_InstallmentIdOrderByTermNumberAsc(savedPlan.getInstallmentId());
            if (existingTerms.size() != savedPlan.getPaymentTerms()) {
                installmentTermRepository.deleteAll(existingTerms);
                terms = generateTerms(savedPlan);
            } else {
                // Just update dates based on new config if start date or frequency changed
                terms = existingTerms;
                updateTermDueDates(savedPlan, terms);
            }
        }

        recalculateTermStatuses(entry.getEntryId());
        terms = installmentTermRepository.findByInstallmentPlan_InstallmentIdOrderByTermNumberAsc(savedPlan.getInstallmentId());

        return convertToDto(savedPlan, terms);
    }

    @Transactional
    public void deletePlanByEntryId(UUID entryId) {
        installmentPlanRepository.findByLoanEntry_EntryId(entryId).ifPresent(plan -> {
            installmentPlanRepository.delete(plan);
        });
    }

    @Transactional
    public void skipTerm(UUID termId) {
        InstallmentTerm term = installmentTermRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Installment term not found with ID: " + termId));
        term.setStatus(InstallmentTermStatus.SKIPPED);
        installmentTermRepository.save(term);

        // Trigger recalculation of remaining terms
        UUID entryId = term.getInstallmentPlan().getLoanEntry().getEntryId();
        recalculateTermStatuses(entryId);
    }

    @Transactional
    public void recalculateTermStatuses(UUID entryId) {
        Optional<InstallmentPlan> planOpt = installmentPlanRepository.findByLoanEntry_EntryId(entryId);
        if (planOpt.isEmpty()) {
            return;
        }
        InstallmentPlan plan = planOpt.get();
        List<InstallmentTerm> terms = installmentTermRepository.findByInstallmentPlan_InstallmentIdOrderByTermNumberAsc(plan.getInstallmentId());
        List<Payment> payments = paymentRepository.findByLoanEntry_EntryId(entryId);

        double totalPaid = payments.stream().mapToDouble(Payment::getPaymentAmount).sum();
        double remainingPaid = totalPaid;
        Date today = new Date();

        // Standardize time component of today to midnight for date-only comparison
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date todayMidnight = todayCal.getTime();

        for (int i = 0; i < terms.size(); i++) {
            InstallmentTerm term = terms.get(i);

            // If term is explicitly skipped, we keep it skipped and do not allocate payment
            if (term.getStatus() == InstallmentTermStatus.SKIPPED) {
                continue;
            }

            double required = plan.getAmountPerTerm() != null ? plan.getAmountPerTerm() : 0.0;

            if (remainingPaid >= required && required > 0) {
                term.setStatus(InstallmentTermStatus.PAID);
                remainingPaid -= required;
            } else {
                // Calculate period start date
                Date rawPeriodStart;
                if (i == 0) {
                    rawPeriodStart = plan.getStartDate();
                } else {
                    rawPeriodStart = terms.get(i - 1).getDueDate();
                }

                Calendar pCal = Calendar.getInstance();
                pCal.setTime(rawPeriodStart);
                pCal.set(Calendar.HOUR_OF_DAY, 0);
                pCal.set(Calendar.MINUTE, 0);
                pCal.set(Calendar.SECOND, 0);
                pCal.set(Calendar.MILLISECOND, 0);
                Date periodStart = pCal.getTime();

                Calendar dCal = Calendar.getInstance();
                dCal.setTime(term.getDueDate());
                dCal.set(Calendar.HOUR_OF_DAY, 0);
                dCal.set(Calendar.MINUTE, 0);
                dCal.set(Calendar.SECOND, 0);
                dCal.set(Calendar.MILLISECOND, 0);
                Date dueDate = dCal.getTime();

                // If term has a partial payment, it remains unpaid or delinquent but does consume the partial amount
                remainingPaid = 0; // consumed

                if (todayMidnight.before(periodStart)) {
                    term.setStatus(InstallmentTermStatus.NOT_STARTED);
                } else if (todayMidnight.after(dueDate)) {
                    term.setStatus(InstallmentTermStatus.DELINQUENT);
                } else {
                    term.setStatus(InstallmentTermStatus.UNPAID);
                }
            }
        }

        installmentTermRepository.saveAll(terms);

        // Update main plan status if all non-skipped terms are paid
        boolean allPaid = terms.stream()
                .filter(t -> t.getStatus() != InstallmentTermStatus.SKIPPED)
                .allMatch(t -> t.getStatus() == InstallmentTermStatus.PAID);
        
        boolean allNotStarted = terms.stream()
                .filter(t -> t.getStatus() != InstallmentTermStatus.SKIPPED)
                .allMatch(t -> t.getStatus() == InstallmentTermStatus.NOT_STARTED);

        boolean hasDelinquent = terms.stream()
                .filter(t -> t.getStatus() != InstallmentTermStatus.SKIPPED)
                .anyMatch(t -> t.getStatus() == InstallmentTermStatus.DELINQUENT);

        LoanEntry entry = plan.getLoanEntry();
        
        if (allPaid && !terms.isEmpty()) {
            plan.setStatus("SETTLED");
            if (entry != null) {
                entry.setPaymentStatus("PAID");
            }
        } else if (allNotStarted && !terms.isEmpty()) {
            plan.setStatus("ACTIVE");
            if (entry != null) {
                entry.setPaymentStatus("NOT STARTED");
            }
        } else if (hasDelinquent && !terms.isEmpty()) {
            plan.setStatus("ACTIVE");
            if (entry != null) {
                entry.setPaymentStatus("DELINQUENT");
            }
        } else {
            plan.setStatus("ACTIVE");
            if (entry != null && ("NOT STARTED".equals(entry.getPaymentStatus()) || "DELINQUENT".equals(entry.getPaymentStatus()))) {
                entry.setPaymentStatus("UNPAID");
            }
        }
        
        if (entry != null) {
            loanEntryRepository.save(entry);
        }
        installmentPlanRepository.save(plan);
    }

    private List<InstallmentTerm> generateTerms(InstallmentPlan plan) {
        List<InstallmentTerm> terms = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(plan.getStartDate());

        for (int i = 1; i <= plan.getPaymentTerms(); i++) {
            // Apply frequency
            String freq = plan.getPaymentFrequency() != null ? plan.getPaymentFrequency().toUpperCase() : "MONTHLY";
            switch (freq) {
                case "DAILY":
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case "WEEKLY":
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    if (plan.getPaymentDay() != null && plan.getPaymentDay() >= 1 && plan.getPaymentDay() <= 7) {
                        calendar.set(Calendar.DAY_OF_WEEK, plan.getPaymentDay());
                    }
                    break;
                case "BI-WEEKLY":
                    calendar.add(Calendar.WEEK_OF_YEAR, 2);
                    if (plan.getPaymentDay() != null && plan.getPaymentDay() >= 1 && plan.getPaymentDay() <= 7) {
                        calendar.set(Calendar.DAY_OF_WEEK, plan.getPaymentDay());
                    }
                    break;
                case "MONTHLY":
                default:
                    calendar.add(Calendar.MONTH, 1);
                    if (plan.getPaymentDay() != null && plan.getPaymentDay() >= 1 && plan.getPaymentDay() <= 28) {
                        calendar.set(Calendar.DAY_OF_MONTH, plan.getPaymentDay());
                    }
                    break;
            }

            InstallmentTerm term = InstallmentTerm.builder()
                    .installmentPlan(plan)
                    .termNumber(i)
                    .dueDate(calendar.getTime())
                    .status(InstallmentTermStatus.NOT_STARTED)
                    .build();

            terms.add(installmentTermRepository.save(term));
        }
        return terms;
    }

    private void updateTermDueDates(InstallmentPlan plan, List<InstallmentTerm> terms) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(plan.getStartDate());

        for (InstallmentTerm term : terms) {
            String freq = plan.getPaymentFrequency() != null ? plan.getPaymentFrequency().toUpperCase() : "MONTHLY";
            switch (freq) {
                case "DAILY":
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case "WEEKLY":
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    if (plan.getPaymentDay() != null && plan.getPaymentDay() >= 1 && plan.getPaymentDay() <= 7) {
                        calendar.set(Calendar.DAY_OF_WEEK, plan.getPaymentDay());
                    }
                    break;
                case "BI-WEEKLY":
                    calendar.add(Calendar.WEEK_OF_YEAR, 2);
                    if (plan.getPaymentDay() != null && plan.getPaymentDay() >= 1 && plan.getPaymentDay() <= 7) {
                        calendar.set(Calendar.DAY_OF_WEEK, plan.getPaymentDay());
                    }
                    break;
                case "MONTHLY":
                default:
                    calendar.add(Calendar.MONTH, 1);
                    if (plan.getPaymentDay() != null && plan.getPaymentDay() >= 1 && plan.getPaymentDay() <= 28) {
                        calendar.set(Calendar.DAY_OF_MONTH, plan.getPaymentDay());
                    }
                    break;
            }
            term.setDueDate(calendar.getTime());
            installmentTermRepository.save(term);
        }
    }

    private InstallmentPlanDto convertToDto(InstallmentPlan plan, List<InstallmentTerm> terms) {
        List<InstallmentTermDto> termDtos = terms.stream()
                .map(t -> InstallmentTermDto.builder()
                        .termId(t.getTermId())
                        .installmentId(plan.getInstallmentId())
                        .termNumber(t.getTermNumber())
                        .dueDate(t.getDueDate())
                        .status(t.getStatus())
                        .notes(t.getNotes())
                        .build())
                .collect(Collectors.toList());

        return InstallmentPlanDto.builder()
                .installmentId(plan.getInstallmentId())
                .entryId(plan.getLoanEntry().getEntryId())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .paymentFrequency(plan.getPaymentFrequency())
                .paymentDay(plan.getPaymentDay())
                .paymentTerms(plan.getPaymentTerms())
                .amountPerTerm(plan.getAmountPerTerm())
                .notes(plan.getNotes())
                .terms(termDtos)
                .build();
    }
}
