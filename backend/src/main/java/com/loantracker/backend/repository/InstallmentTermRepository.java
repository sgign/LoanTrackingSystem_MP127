package com.loantracker.backend.repository;

import com.loantracker.backend.entity.InstallmentTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentTermRepository extends JpaRepository<InstallmentTerm, UUID> {
    List<InstallmentTerm> findByInstallmentPlan_InstallmentIdOrderByTermNumberAsc(UUID installmentId);
}
