package com.loantracker.backend.controller;

import com.loantracker.backend.dto.LoanEntryDto;
import com.loantracker.backend.dto.PersonRequest;
import com.loantracker.backend.dto.PersonResponse;
import com.loantracker.backend.service.LoanEntryService;
import com.loantracker.backend.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;
    private final LoanEntryService loanEntryService;

    @GetMapping
    public List<PersonResponse> getAllPeople() {
        return personService.getAllPersons();
    }

    @GetMapping("/{id}")
    public PersonResponse getPersonById(@PathVariable("id") UUID id) {
        return personService.getPersonById(id);
    }

    @GetMapping("/{id}/entries")
    public List<LoanEntryDto> getEntriesByPersonId(@PathVariable("id") UUID id) {
        return loanEntryService.getLoansByPersonId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse createPerson(@RequestBody PersonRequest request) {
        return personService.createPerson(request);
    }

    @PutMapping("/{id}")
    public PersonResponse updatePerson(@PathVariable("id") UUID id, @RequestBody PersonRequest request) {
        return personService.updatePerson(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePerson(@PathVariable("id") UUID id) {
        personService.deletePerson(id);
    }
}
