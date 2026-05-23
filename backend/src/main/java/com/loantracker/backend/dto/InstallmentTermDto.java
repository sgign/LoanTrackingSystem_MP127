package com.loantracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.loantracker.backend.entity.InstallmentTermStatus;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentTermDto {
    private UUID termId;
    private UUID installmentId;
    private Integer termNumber;
    private Date dueDate;
    private InstallmentTermStatus status;
    private String notes;
}
