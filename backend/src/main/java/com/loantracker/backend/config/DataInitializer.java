package com.loantracker.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        Integer personCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM person", Integer.class);
        if (personCount != null && personCount > 0) {
            return; // Already initialized
        }

        // Insert Persons
        jdbcTemplate.update("INSERT INTO person (person_id, first_name, last_name, initials, contact_info, notes) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "David Jonathan", "Pasumbal", "DJP", "david.p@example.com", "Primary user of the system");
        jdbcTemplate.update("INSERT INTO person (person_id, first_name, last_name, initials, contact_info, notes) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.fromString("22222222-2222-2222-2222-222222222222"), "Shanessa", "Tugaoen", "ST", "shanessa.t@example.com", "Colleague from work");
        jdbcTemplate.update("INSERT INTO person (person_id, first_name, last_name, initials, contact_info, notes) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.fromString("33333333-3333-3333-3333-333333333333"), "Sabrina", "Ignacio", "SI", "sabrina.i@example.com", "College classmate");
        jdbcTemplate.update("INSERT INTO person (person_id, first_name, last_name, initials, contact_info, notes) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.fromString("44444444-4444-4444-4444-444444444444"), "Razz", "Rios", "RR", "razz.r@example.com", "Gym partner");
        jdbcTemplate.update("INSERT INTO person (person_id, first_name, last_name, initials, contact_info, notes) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.fromString("55555555-5555-5555-5555-555555555555"), "Aisea", "Vidal", "AV", "aisea.v@example.com", "High school friend");
        jdbcTemplate.update("INSERT INTO person (person_id, first_name, last_name, initials, contact_info, notes) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.fromString("66666666-6666-6666-6666-666666666666"), "Ani", "Sibal", "AS", "ani.s@example.com", "Neighbor");

        // Insert Groups
        jdbcTemplate.update("INSERT INTO group_entity (group_id, group_name, notes) VALUES (?, ?, ?)",
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), "Work Friends", "Group of colleagues from work projects");
        jdbcTemplate.update("INSERT INTO group_entity (group_id, group_name, notes) VALUES (?, ?, ?)",
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Family", "Immediate family members");
        jdbcTemplate.update("INSERT INTO group_entity (group_id, group_name, notes) VALUES (?, ?, ?)",
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"), "College Circle", "Close friends from college days");

        // Insert Loan Entries
        jdbcTemplate.update("INSERT INTO loan_entry (entry_id, reference_id, entry_name, transaction_type, borrower_person_id, lender_person_id, amount_borrowed, amount_remaining, payment_status, date_borrowed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)",
                UUID.randomUUID(), "DJP-ST", "Lunch at Jollibee", "Straight Expense", UUID.fromString("11111111-1111-1111-1111-111111111111"), UUID.fromString("22222222-2222-2222-2222-222222222222"), 500.00, 500.00, "UNPAID");

        jdbcTemplate.update("INSERT INTO loan_entry (entry_id, reference_id, entry_name, transaction_type, borrower_person_id, lender_person_id, amount_borrowed, amount_remaining, payment_status, date_borrowed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)",
                UUID.randomUUID(), "SI-DJP", "Grab Fare", "Straight Expense", UUID.fromString("33333333-3333-3333-3333-333333333333"), UUID.fromString("11111111-1111-1111-1111-111111111111"), 250.00, 100.00, "PARTIALLY PAID");

        jdbcTemplate.update("INSERT INTO loan_entry (entry_id, reference_id, entry_name, transaction_type, borrower_group_id, lender_person_id, amount_borrowed, amount_remaining, payment_status, date_borrowed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)",
                UUID.randomUUID(), "WF-DJP", "Team Dinner", "Group Expense", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), UUID.fromString("11111111-1111-1111-1111-111111111111"), 1500.00, 1500.00, "UNPAID");
    }
}
