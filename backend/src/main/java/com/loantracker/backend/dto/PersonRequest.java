package com.loantracker.backend.dto;

import lombok.Data;

@Data
public class PersonRequest {
    private String firstName;
    private String lastName;
    private String contactInfo;
    private String notes;
}
