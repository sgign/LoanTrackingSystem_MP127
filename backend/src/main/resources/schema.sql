-- ==========================================
-- Database Schema DDL for Loan Tracking System
-- Target: PostgreSQL
-- ==========================================

-- 1. Person Table
CREATE TABLE person (
    person_id UUID PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    initials VARCHAR(50),
    contact_info VARCHAR(255),
    notes VARCHAR(1000)
);

-- 2. Group Entity Table
CREATE TABLE group_entity (
    group_id UUID PRIMARY KEY,
    group_name VARCHAR(255) NOT NULL,
    notes VARCHAR(1000)
);

-- 3. Group Membership Table (Many-to-Many Bridge)
CREATE TABLE group_membership (
    membership_id UUID PRIMARY KEY,
    person_id UUID NOT NULL,
    group_id UUID NOT NULL,
    CONSTRAINT fk_group_membership_person FOREIGN KEY (person_id) REFERENCES person (person_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_membership_group FOREIGN KEY (group_id) REFERENCES group_entity (group_id) ON DELETE CASCADE
);

-- 4. Loan Entry Table
CREATE TABLE loan_entry (
    entry_id UUID PRIMARY KEY,
    reference_id VARCHAR(255),
    entry_name VARCHAR(255),
    description VARCHAR(1000),
    transaction_type VARCHAR(100),
    date_borrowed DATE,
    date_fully_paid DATE,
    borrower_person_id UUID,
    borrower_group_id UUID,
    lender_person_id UUID,
    amount_borrowed DECIMAL(15, 2),
    amount_remaining DECIMAL(15, 2),
    payment_status VARCHAR(50),
    notes VARCHAR(1000),
    payment_notes VARCHAR(1000),
    receipt_proof BYTEA,
    CONSTRAINT fk_loan_entry_borrower_person FOREIGN KEY (borrower_person_id) REFERENCES person (person_id) ON DELETE SET NULL,
    CONSTRAINT fk_loan_entry_borrower_group FOREIGN KEY (borrower_group_id) REFERENCES group_entity (group_id) ON DELETE SET NULL,
    CONSTRAINT fk_loan_entry_lender_person FOREIGN KEY (lender_person_id) REFERENCES person (person_id) ON DELETE SET NULL
);

-- 5. Payment Table
CREATE TABLE payment (
    payment_id UUID PRIMARY KEY,
    entry_id UUID NOT NULL,
    payment_date TIMESTAMP,
    payment_amount DECIMAL(15, 2),
    payee_person_id UUID,
    proof BYTEA,
    notes VARCHAR(1000),
    CONSTRAINT fk_payment_loan_entry FOREIGN KEY (entry_id) REFERENCES loan_entry (entry_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_payee_person FOREIGN KEY (payee_person_id) REFERENCES person (person_id) ON DELETE SET NULL
);

-- 6. Installment Plan Table
CREATE TABLE installment_plan (
    installment_id UUID PRIMARY KEY,
    entry_id UUID NOT NULL,
    status VARCHAR(50),
    start_date DATE,
    payment_frequency VARCHAR(50),
    payment_day INTEGER,
    payment_terms INTEGER,
    amount_per_term DECIMAL(15, 2),
    notes VARCHAR(1000),
    CONSTRAINT fk_installment_plan_loan_entry FOREIGN KEY (entry_id) REFERENCES loan_entry (entry_id) ON DELETE CASCADE
);

-- 7. Installment Term Table
CREATE TABLE installment_term (
    term_id UUID PRIMARY KEY,
    installment_id UUID NOT NULL,
    term_number INTEGER,
    due_date DATE,
    status VARCHAR(50),
    notes VARCHAR(1000),
    CONSTRAINT fk_installment_term_plan FOREIGN KEY (installment_id) REFERENCES installment_plan (installment_id) ON DELETE CASCADE
);

-- 8. Payment Allocation Table
CREATE TABLE payment_allocation (
    allocation_id UUID PRIMARY KEY,
    entry_id UUID NOT NULL,
    description VARCHAR(1000),
    payee_person_id UUID,
    amount DECIMAL(15, 2),
    percentage_of_total DECIMAL(5, 2),
    status VARCHAR(50),
    notes VARCHAR(1000),
    CONSTRAINT fk_payment_allocation_loan_entry FOREIGN KEY (entry_id) REFERENCES loan_entry (entry_id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_allocation_payee_person FOREIGN KEY (payee_person_id) REFERENCES person (person_id) ON DELETE SET NULL
);
