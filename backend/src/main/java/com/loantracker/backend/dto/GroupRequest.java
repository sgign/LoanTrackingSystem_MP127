package com.loantracker.backend.dto;

import lombok.Data;

@Data
public class GroupRequest {
    private String groupName;
    private String notes;
}
