package com.loantracker.backend.repository;

import com.loantracker.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID; 

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByLoanEntry_EntryId(UUID entryId);
}