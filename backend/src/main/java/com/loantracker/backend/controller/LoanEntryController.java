package com.loantracker.backend.controller;

import com.loantracker.backend.dto.LoanEntryDto;
import com.loantracker.backend.service.LoanEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class LoanEntryController {

    private final LoanEntryService loanEntryService;

    @GetMapping
    public List<LoanEntryDto> getAllLoans() {
        return loanEntryService.getAllLoans();
    }

    @GetMapping("/{id}")
    public LoanEntryDto getLoanById(@PathVariable("id") UUID id) {
        return loanEntryService.getLoanById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanEntryDto createLoan(@RequestBody LoanEntryDto dto) {
        return loanEntryService.createLoan(dto);
    }

    @PutMapping("/{id}")
    public LoanEntryDto updateLoan(@PathVariable("id") UUID id, @RequestBody LoanEntryDto dto) {
        return loanEntryService.updateLoan(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLoan(@PathVariable("id") UUID id) {
        loanEntryService.deleteLoan(id);
    }
}
