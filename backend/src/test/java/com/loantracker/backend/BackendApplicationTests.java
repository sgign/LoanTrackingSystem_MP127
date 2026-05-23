package com.loantracker.backend;

import com.loantracker.backend.dto.LoanEntryDto;
import com.loantracker.backend.entity.GroupEntity;
import com.loantracker.backend.entity.Person;
import com.loantracker.backend.repository.GroupEntityRepository;
import com.loantracker.backend.repository.PersonRepository;
import com.loantracker.backend.service.LoanEntryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BackendApplicationTests {

	@Autowired
	private LoanEntryService loanEntryService;

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private GroupEntityRepository groupEntityRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void testResolveAndCreateLoan() {
		// 1. Create a DTO with names
		LoanEntryDto dto = LoanEntryDto.builder()
				.entryName("Test Loan")
				.transactionType("Straight Expense")
				.dateBorrowed(new Date())
				.borrowerPersonName("Jane Doe")
				.lenderPersonName("David Jonathan Pasumbal")
				.amountBorrowed(150.0)
				.build();

		// 2. Call service method to save/create loan
		LoanEntryDto created = loanEntryService.createLoan(dto);

		// 3. Assertions on the created loan DTO
		assertNotNull(created.getEntryId());
		assertEquals("JD-DJP-1", created.getReferenceId());
		assertEquals("Jane Doe", created.getBorrowerPersonName());
		assertEquals("David Jonathan Pasumbal", created.getLenderPersonName());
		assertEquals(150.0, created.getAmountBorrowed());
		assertEquals(150.0, created.getAmountRemaining());
		assertEquals("UNPAID", created.getPaymentStatus());

		// 4. Assertions on database
		Optional<Person> borrowerOpt = personRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Jane", "Doe");
		assertTrue(borrowerOpt.isPresent());
		Person borrower = borrowerOpt.get();
		assertEquals("Jane", borrower.getFirstName());
		assertEquals("Doe", borrower.getLastName());
		assertEquals("JD", borrower.getInitials());

		// 5. Test case-insensitive resolution (should not duplicate "Jane Doe")
		LoanEntryDto secondDto = LoanEntryDto.builder()
				.entryName("Second Test Loan")
				.transactionType("Straight Expense")
				.dateBorrowed(new Date())
				.borrowerPersonName("jane doe") // same name, lowercase
				.lenderPersonName("david jonathan pasumbal")
				.amountBorrowed(50.0)
				.build();

		LoanEntryDto secondCreated = loanEntryService.createLoan(secondDto);
		assertEquals("JD-DJP-2", secondCreated.getReferenceId());
		assertEquals(borrower.getPersonId(), secondCreated.getBorrowerPersonId());

		// 6. Test borrower group resolution
		LoanEntryDto groupDto = LoanEntryDto.builder()
				.entryName("Group Loan")
				.transactionType("Group Expense")
				.dateBorrowed(new Date())
				.borrowerGroupName("Awesome Team")
				.lenderPersonName("David Jonathan Pasumbal")
				.amountBorrowed(300.0)
				.build();

		LoanEntryDto groupCreated = loanEntryService.createLoan(groupDto);
		assertEquals("AT-DJP-1", groupCreated.getReferenceId());

		Optional<GroupEntity> groupOpt = groupEntityRepository.findByGroupNameIgnoreCase("Awesome Team");
		assertTrue(groupOpt.isPresent());
		GroupEntity group = groupOpt.get();
		assertEquals("Awesome Team", group.getGroupName());
	}

}
