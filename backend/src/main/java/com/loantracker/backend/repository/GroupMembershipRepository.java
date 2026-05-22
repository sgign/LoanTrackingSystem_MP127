package com.loantracker.backend.repository;

import com.loantracker.backend.entity.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, UUID> {

    List<GroupMembership> findByGroup_GroupId(UUID groupId);

    List<GroupMembership> findByPerson_PersonId(UUID personId);

    boolean existsByPerson_PersonIdAndGroup_GroupId(UUID personId, UUID groupId);
}
