package com.loantracker.backend.service;

import com.loantracker.backend.dto.GroupRequest;
import com.loantracker.backend.dto.GroupResponse;
import com.loantracker.backend.dto.PersonResponse;
import com.loantracker.backend.entity.GroupEntity;
import com.loantracker.backend.entity.GroupMembership;
import com.loantracker.backend.repository.GroupEntityRepository;
import com.loantracker.backend.repository.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupEntityRepository groupEntityRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final PersonService personService;

    public GroupResponse createGroup(GroupRequest request) {
        GroupEntity group = new GroupEntity();
        group.setGroupName(request.getGroupName());
        group.setNotes(request.getNotes());

        GroupEntity saved = groupEntityRepository.save(group);
        return mapToResponse(saved);
    }

    public List<GroupResponse> getAllGroups() {
        return groupEntityRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroupById(UUID id) {
        GroupEntity group = groupEntityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return mapToResponse(group);
    }

    public GroupResponse updateGroup(UUID id, GroupRequest request) {
        GroupEntity group = groupEntityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.setGroupName(request.getGroupName());
        group.setNotes(request.getNotes());

        GroupEntity updated = groupEntityRepository.save(group);
        return mapToResponse(updated);
    }

    public void deleteGroup(UUID id) {
        if (!groupEntityRepository.existsById(id)) {
            throw new RuntimeException("Group not found");
        }
        // First delete all memberships for this group to avoid constraint violations
        List<GroupMembership> memberships = groupMembershipRepository.findByGroup_GroupId(id);
        groupMembershipRepository.deleteAll(memberships);
        
        groupEntityRepository.deleteById(id);
    }

    private GroupResponse mapToResponse(GroupEntity group) {
        List<PersonResponse> members = groupMembershipRepository.findByGroup_GroupId(group.getGroupId())
                .stream()
                .map(GroupMembership::getPerson)
                .map(personService::mapToResponse)
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .notes(group.getNotes())
                .members(members)
                .build();
    }
}
