package com.loantracker.backend.repository;

import com.loantracker.backend.entity.LoanEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanEntryRepository extends JpaRepository<LoanEntry, UUID> {

    @Query("SELECT l FROM LoanEntry l WHERE l.borrowerPerson.personId = :personId OR l.lenderPerson.personId = :personId")
    List<LoanEntry> findByPersonId(@Param("personId") UUID personId);

    @Query("SELECT l FROM LoanEntry l WHERE l.borrowerGroup.groupId = :groupId")
    List<LoanEntry> findByGroupId(@Param("groupId") UUID groupId);
}
