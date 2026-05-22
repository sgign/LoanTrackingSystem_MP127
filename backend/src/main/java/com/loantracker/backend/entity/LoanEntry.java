package com.loantracker.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "loan_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LoanEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entry_id")
    private UUID entryId;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "entry_name")
    private String entryName;

    @Column(name = "description")
    private String description;

    @Column(name = "transaction_type")
    private String transactionType;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_borrowed")
    private Date dateBorrowed;

    @Temporal(TemporalType.DATE)
    @Column(name = "date_fully_paid")
    private Date dateFullyPaid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_person_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Person borrowerPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_group_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private GroupEntity borrowerGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lender_person_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Person lenderPerson;

    @Column(name = "amount_borrowed")
    private Double amountBorrowed;

    @Column(name = "amount_remaining")
    private Double amountRemaining;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "notes")
    private String notes;

    @Column(name = "payment_notes")
    private String paymentNotes;

    @Column(name = "receipt_proof", columnDefinition = "BYTEA")
    private byte[] receiptProof;

    @Column(name = "split_type")
    private String splitType;
}
