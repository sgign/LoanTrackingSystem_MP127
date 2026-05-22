package com.loantracker.backend.entity;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entry_id", nullable = false)
    // Prevents infinite loops when converting to JSON
    @JsonIgnoreProperties({"payments", "hibernateLazyInitializer", "handler"}) 
    private LoanEntry loanEntry;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "payment_date")
    private Date paymentDate;

    @Column(name = "payment_amount")
    private Double paymentAmount;

    // Fixed: Standardizing on EAGER for dashboard summaries
    @ManyToOne(fetch = FetchType.EAGER) 
    @JoinColumn(name = "payee_person_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Person payeePerson;

    @Column(name = "proof", columnDefinition = "BYTEA")
    private byte[] proof;

    @Column(name = "notes", columnDefinition = "TEXT") // Changed to TEXT for safer storage
    private String notes;
}