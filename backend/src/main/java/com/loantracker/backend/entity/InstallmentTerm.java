package com.loantracker.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "installment_term")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "term_id")
    private UUID termId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_id", nullable = false)
    private InstallmentPlan installmentPlan;

    @Column(name = "term_number")
    private Integer termNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "status")
    private InstallmentTermStatus status;

    @Column(name = "notes")
    private String notes;
}
