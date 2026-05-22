package com.loantracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentPlanDto {
    private UUID installmentId;
    private UUID entryId;
    private String status;
    private Date startDate;
    private String paymentFrequency;
    private Integer paymentDay;
    private Integer paymentTerms;
    private Double amountPerTerm;
    private String notes;
    private List<InstallmentTermDto> terms;
}
