package com.loantracker.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "installment_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "installment_id")
    private UUID installmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private LoanEntry loanEntry;

    @Column(name = "status")
    private String status;

    @Temporal(TemporalType.DATE)
    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "payment_frequency")
    private String paymentFrequency;

    @Column(name = "payment_day")
    private Integer paymentDay;

    @Column(name = "payment_terms")
    private Integer paymentTerms;

    @Column(name = "amount_per_term")
    private Double amountPerTerm;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "installmentPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<InstallmentTerm> installmentTerms;
}
