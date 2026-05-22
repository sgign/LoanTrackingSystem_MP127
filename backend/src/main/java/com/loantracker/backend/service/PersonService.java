package com.loantracker.backend.service;

import com.loantracker.backend.dto.PersonRequest;
import com.loantracker.backend.dto.PersonResponse;
import com.loantracker.backend.entity.GroupMembership;
import com.loantracker.backend.entity.Person;
import com.loantracker.backend.repository.GroupMembershipRepository;
import com.loantracker.backend.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public PersonResponse createPerson(PersonRequest request) {
        Person person = new Person();
        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setContactInfo(request.getContactInfo());
        person.setNotes(request.getNotes());
        person.setInitials(generateInitials(request.getFirstName(), request.getLastName()));

        Person saved = personRepository.save(person);
        return mapToResponse(saved);
    }

    public List<PersonResponse> getAllPersons() {
        return personRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PersonResponse getPersonById(UUID id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        return mapToResponse(person);
    }

    public PersonResponse updatePerson(UUID id, PersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));

        person.setFirstName(request.getFirstName());
        person.setLastName(request.getLastName());
        person.setContactInfo(request.getContactInfo());
        person.setNotes(request.getNotes());
        person.setInitials(generateInitials(request.getFirstName(), request.getLastName()));

        Person updated = personRepository.save(person);
        return mapToResponse(updated);
    }

    public void deletePerson(UUID id) {
        if (!personRepository.existsById(id)) {
            throw new RuntimeException("Person not found");
        }
        
        // Remove all group memberships for this person first to avoid constraint violations
        List<GroupMembership> memberships = groupMembershipRepository.findByPerson_PersonId(id);
        groupMembershipRepository.deleteAll(memberships);
        
        personRepository.deleteById(id);
    }

    private String generateInitials(String firstName, String lastName) {
        String first = (firstName != null && !firstName.isBlank()) ? firstName.trim().substring(0, 1).toUpperCase() : "";
        String last = (lastName != null && !lastName.isBlank()) ? lastName.trim().substring(0, 1).toUpperCase() : "";
        return first + last;
    }

    public PersonResponse mapToResponse(Person person) {
        return PersonResponse.builder()
                .personId(person.getPersonId())
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .initials(person.getInitials())
                .contactInfo(person.getContactInfo())
                .notes(person.getNotes())
                .build();
    }
}
