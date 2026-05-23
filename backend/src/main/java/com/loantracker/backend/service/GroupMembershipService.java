package com.loantracker.backend.service;

import com.loantracker.backend.dto.GroupMembershipRequest;
import com.loantracker.backend.dto.GroupMembershipResponse;
import com.loantracker.backend.entity.GroupEntity;
import com.loantracker.backend.entity.GroupMembership;
import com.loantracker.backend.entity.Person;
import com.loantracker.backend.repository.GroupEntityRepository;
import com.loantracker.backend.repository.GroupMembershipRepository;
import com.loantracker.backend.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupMembershipService {

    private final GroupMembershipRepository groupMembershipRepository;
    private final PersonRepository personRepository;
    private final GroupEntityRepository groupEntityRepository;

    public GroupMembershipResponse addMember(GroupMembershipRequest request) {
        if (groupMembershipRepository.existsByPerson_PersonIdAndGroup_GroupId(request.getPersonId(), request.getGroupId())) {
            throw new RuntimeException("Person is already a member of this group");
        }

        Person person = personRepository.findById(request.getPersonId())
                .orElseThrow(() -> new RuntimeException("Person not found"));
        GroupEntity group = groupEntityRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        GroupMembership membership = new GroupMembership();
        membership.setPerson(person);
        membership.setGroup(group);

        GroupMembership saved = groupMembershipRepository.save(membership);
        return mapToResponse(saved);
    }

    public void removeMember(UUID membershipId) {
        if (!groupMembershipRepository.existsById(membershipId)) {
            throw new RuntimeException("Membership not found");
        }
        groupMembershipRepository.deleteById(membershipId);
    }

    public List<GroupMembershipResponse> getMembersByGroup(UUID groupId) {
        return groupMembershipRepository.findByGroup_GroupId(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private GroupMembershipResponse mapToResponse(GroupMembership membership) {
        return GroupMembershipResponse.builder()
                .membershipId(membership.getMembershipId())
                .groupId(membership.getGroup().getGroupId())
                .groupName(membership.getGroup().getGroupName())
                .personId(membership.getPerson().getPersonId())
                .personFirstName(membership.getPerson().getFirstName())
                .personLastName(membership.getPerson().getLastName())
                .personInitials(membership.getPerson().getInitials())
                .build();
    }
}
