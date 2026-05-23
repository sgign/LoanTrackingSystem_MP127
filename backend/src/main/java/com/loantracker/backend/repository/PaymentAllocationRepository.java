package com.loantracker.backend.repository;

import com.loantracker.backend.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {
    List<PaymentAllocation> findByLoanEntry_EntryId(UUID entryId);
    void deleteByLoanEntry_EntryId(UUID entryId);
}
