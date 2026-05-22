package com.loantracker.backend.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class GroupMembershipRequest {
    private UUID personId;
    private UUID groupId;
}
