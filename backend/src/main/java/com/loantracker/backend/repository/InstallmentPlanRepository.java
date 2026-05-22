package com.loantracker.backend.repository;

import com.loantracker.backend.entity.InstallmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, UUID> {
    Optional<InstallmentPlan> findByLoanEntry_EntryId(UUID entryId);
}
