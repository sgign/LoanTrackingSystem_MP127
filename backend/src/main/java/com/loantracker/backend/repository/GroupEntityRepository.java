package com.loantracker.backend.repository;

import com.loantracker.backend.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupEntityRepository extends JpaRepository<GroupEntity, UUID> {
    Optional<GroupEntity> findByGroupNameIgnoreCase(String groupName);
}

