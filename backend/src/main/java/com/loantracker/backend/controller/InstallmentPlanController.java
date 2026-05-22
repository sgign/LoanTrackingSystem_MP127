package com.loantracker.backend.controller;

import com.loantracker.backend.dto.InstallmentPlanDto;
import com.loantracker.backend.service.InstallmentPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/installments")
@RequiredArgsConstructor
public class InstallmentPlanController {

    private final InstallmentPlanService installmentPlanService;

    @GetMapping("/entry/{entryId}")
    public InstallmentPlanDto getPlanByEntryId(@PathVariable("entryId") UUID entryId) {
        return installmentPlanService.getPlanByEntryId(entryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstallmentPlanDto savePlan(@RequestBody InstallmentPlanDto dto) {
        return installmentPlanService.savePlan(dto);
    }

    @PostMapping("/terms/{termId}/skip")
    public void skipTerm(@PathVariable("termId") UUID termId) {
        installmentPlanService.skipTerm(termId);
    }
}
