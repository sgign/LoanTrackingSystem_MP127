package com.loantracker.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private LoanEntry loanEntry;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "payment_date")
    private Date paymentDate;

    @Column(name = "payment_amount")
    private Double paymentAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_person_id")
    private Person payeePerson;

    @Column(name = "proof", columnDefinition = "BYTEA")
    private byte[] proof;

    @Column(name = "notes")
    private String notes;
}
