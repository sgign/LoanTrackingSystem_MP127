package com.loantracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanEntryDto {
    private UUID entryId;
    private String referenceId;
    private String entryName;
    private String description;
    private String transactionType;
    private Date dateBorrowed;
    private Date dateFullyPaid;
    private UUID borrowerPersonId;
    private String borrowerPersonName;
    private UUID borrowerGroupId;
    private String borrowerGroupName;
    private UUID lenderPersonId;
    private String lenderPersonName;
    private Double amountBorrowed;
    private Double amountRemaining;
    private String paymentStatus;
    private String notes;
    private String paymentNotes;
    private String receiptProofBase64;
}
