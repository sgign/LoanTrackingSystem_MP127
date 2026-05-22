package com.loantracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocationDto {
    private UUID allocationId;
    private UUID entryId;
    private String description;
    private UUID payeePersonId;
    private String payeePersonName;
    private Double amount;
    private Double percentageOfTotal;
    private String status;
    private String notes;
}
