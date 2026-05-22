package com.loantracker.backend.controller;

import com.loantracker.backend.dto.GroupRequest;
import com.loantracker.backend.dto.GroupResponse;
import com.loantracker.backend.dto.LoanEntryDto;
import com.loantracker.backend.service.GroupService;
import com.loantracker.backend.service.LoanEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final LoanEntryService loanEntryService;

    @GetMapping
    public List<GroupResponse> getAllGroups() {
        return groupService.getAllGroups();
    }

    @GetMapping("/{id}")
    public GroupResponse getGroupById(@PathVariable("id") UUID id) {
        return groupService.getGroupById(id);
    }

    @GetMapping("/{id}/entries")
    public List<LoanEntryDto> getEntriesByGroupId(@PathVariable("id") UUID id) {
        return loanEntryService.getLoansByGroupId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(@RequestBody GroupRequest request) {
        return groupService.createGroup(request);
    }

    @PutMapping("/{id}")
    public GroupResponse updateGroup(@PathVariable("id") UUID id, @RequestBody GroupRequest request) {
        return groupService.updateGroup(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable("id") UUID id) {
        groupService.deleteGroup(id);
    }
}
