package com.loantracker.backend.controller;

import com.loantracker.backend.dto.PaymentAllocationDto;
import com.loantracker.backend.service.PaymentAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
public class PaymentAllocationController {

    private final PaymentAllocationService paymentAllocationService;

    @GetMapping("/entry/{entryId}")
    public List<PaymentAllocationDto> getAllocationsByEntryId(@PathVariable("entryId") UUID entryId) {
        return paymentAllocationService.getAllocationsByEntryId(entryId);
    }

    @PostMapping("/split")
    public List<PaymentAllocationDto> previewSplit(
            @RequestParam("totalAmount") Double totalAmount,
            @RequestParam("splitType") String splitType,
            @RequestBody List<PaymentAllocationDto> inputs) {
        return paymentAllocationService.calculateSplit(totalAmount, inputs, splitType);
    }
}
