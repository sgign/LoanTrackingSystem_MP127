package com.loantracker.backend.controller;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.loantracker.backend.entity.LoanEntry;
import com.loantracker.backend.entity.Payment;
import com.loantracker.backend.repository.LoanEntryRepository;
import com.loantracker.backend.repository.PaymentRepository;
import com.loantracker.backend.service.PaymentAllocationService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private LoanEntryRepository loanEntryRepository;
    @Autowired
    private PaymentAllocationService paymentAllocationService;

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @PostMapping("/add")
    public ResponseEntity<?> createPayment(@RequestBody Payment payment) {
        try {
            if (payment.getLoanEntry() == null || payment.getLoanEntry().getEntryId() == null) {
                return ResponseEntity.badRequest().body("loanEntry with a valid entryId is required.");
            }
            UUID entryId = payment.getLoanEntry().getEntryId();
            LoanEntry loanEntry = loanEntryRepository.findById(entryId)
                    .orElseThrow(() -> new IllegalArgumentException("Loan entry not found with ID: " + entryId));
            payment.setLoanEntry(loanEntry);
            if (payment.getPaymentDate() == null) {
                payment.setPaymentDate(new java.util.Date());
            }
            Payment saved = paymentRepository.save(payment);
            double currentRemaining = loanEntry.getAmountRemaining() != null ? loanEntry.getAmountRemaining() : 0.0;
            double paymentAmount = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : 0.0;
            double newRemaining = Math.max(0.0, currentRemaining - paymentAmount);
            loanEntry.setAmountRemaining(newRemaining);
            double amountBorrowed = loanEntry.getAmountBorrowed() != null ? loanEntry.getAmountBorrowed() : 0.0;
            if (newRemaining <= 0) {
                loanEntry.setPaymentStatus("PAID");
                if (loanEntry.getDateFullyPaid() == null) {
                    loanEntry.setDateFullyPaid(new java.util.Date());
                }
            } else if (newRemaining < amountBorrowed) {
                loanEntry.setPaymentStatus("PARTIALLY PAID");
                loanEntry.setDateFullyPaid(null);
            }
            loanEntryRepository.save(loanEntry);
            paymentAllocationService.recalculateAllocationStatuses(entryId);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save payment: " + e.getMessage());
        }
    }

    // Delete a single payment and reverse its amount back to the loan entry
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<?> deletePayment(@PathVariable UUID paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));
            LoanEntry loanEntry = payment.getLoanEntry();
            if (loanEntry != null) {
                double paymentAmount = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : 0.0;
                double currentRemaining = loanEntry.getAmountRemaining() != null ? loanEntry.getAmountRemaining() : 0.0;
                double amountBorrowed = loanEntry.getAmountBorrowed() != null ? loanEntry.getAmountBorrowed() : 0.0;
                double newRemaining = Math.min(amountBorrowed, currentRemaining + paymentAmount);
                loanEntry.setAmountRemaining(newRemaining);
                if (newRemaining <= 0) {
                    loanEntry.setPaymentStatus("PAID");
                } else if (newRemaining < amountBorrowed) {
                    loanEntry.setPaymentStatus("PARTIALLY PAID");
                    loanEntry.setDateFullyPaid(null);
                } else {
                    loanEntry.setPaymentStatus("UNPAID");
                    loanEntry.setDateFullyPaid(null);
                }
                loanEntryRepository.save(loanEntry);
            }
            paymentRepository.deleteById(paymentId);
            if (loanEntry != null) {
                paymentAllocationService.recalculateAllocationStatuses(loanEntry.getEntryId());
            }
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete payment: " + e.getMessage());
        }
    }

    // Delete all payments for a given loan entry (used before deleting the entry itself)
    @DeleteMapping("/by-entry/{entryId}")
    public ResponseEntity<?> deleteAllPaymentsForEntry(@PathVariable UUID entryId) {
        try {
            List<Payment> payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getLoanEntry() != null && entryId.equals(p.getLoanEntry().getEntryId()))
                    .toList();
            paymentRepository.deleteAll(payments);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete payments: " + e.getMessage());
        }
    }
}