package com.loantracker.backend.controller;

import com.loantracker.backend.dto.GroupMembershipRequest;
import com.loantracker.backend.dto.GroupMembershipResponse;
import com.loantracker.backend.service.GroupMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GroupMembershipController {

    private final GroupMembershipService groupMembershipService;

    @GetMapping("/api/groups/{groupId}/members")
    public List<GroupMembershipResponse> getMembersByGroup(@PathVariable("groupId") UUID groupId) {
        return groupMembershipService.getMembersByGroup(groupId);
    }

    @PostMapping("/api/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMembershipResponse addMember(@RequestBody GroupMembershipRequest request) {
        return groupMembershipService.addMember(request);
    }

    @DeleteMapping("/api/memberships/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable("membershipId") UUID membershipId) {
        groupMembershipService.removeMember(membershipId);
    }
}
