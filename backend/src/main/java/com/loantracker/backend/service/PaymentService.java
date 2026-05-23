package com.loantracker.backend.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.loantracker.backend.entity.Payment;
import com.loantracker.backend.repository.PaymentRepository;

@Service
public class PaymentService {
    
    @Autowired
    private PaymentRepository paymentRepository;

    public Payment savePayment(Payment payment) {
        if (payment.getLoanEntry() != null && payment.getLoanEntry().getDateBorrowed() != null) {
            Date borrowedDate = payment.getLoanEntry().getDateBorrowed();
            Date paidDate = payment.getPaymentDate();

            if (paidDate != null && paidDate.before(borrowedDate)) {
                throw new IllegalArgumentException("Payment date cannot be before the loan date!");
            }
        }
        return paymentRepository.save(payment);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}