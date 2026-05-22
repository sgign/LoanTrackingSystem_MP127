package com.loantracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembershipResponse {
    private UUID membershipId;
    private UUID groupId;
    private String groupName;
    private UUID personId;
    private String personFirstName;
    private String personLastName;
    private String personInitials;
}
