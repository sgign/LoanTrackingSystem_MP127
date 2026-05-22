package com.loantracker.backend.repository;

import com.loantracker.backend.entity.LoanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LoanEntryRepository extends JpaRepository<LoanEntry, UUID> {
}
