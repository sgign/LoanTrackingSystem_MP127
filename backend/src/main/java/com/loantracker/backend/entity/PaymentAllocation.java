package com.loantracker.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "payment_allocation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id")
    private UUID allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private LoanEntry loanEntry;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_person_id")
    private Person payeePerson;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "percentage_of_total")
    private Double percentageOfTotal;

    @Column(name = "status")
    private String status;

    @Column(name = "notes")
    private String notes;
}
