# Loan Tracking System

A personal finance web application for tracking loans, debts, installment payments, and shared group expenses. Designed for a single user to manage who owes them money and what they owe others.

---

## Features

### Transaction Types
- **Straight Expense** — A one-time loan paid by a single person
- **Installment Expense** — A loan paid in recurring installments (weekly or monthly)
- **Group Expense** — A shared expense split among a group of people

### Entry Management (CRUD)
- Create entries for money loaned out or borrowed
- Create installment entries with configurable payment terms and frequency
- Create shared group expenses with flexible cost splitting
- View, modify, and delete any entry
- Auto-complete entries once fully paid

### Payments
- Log partial or full payments against any entry
- Track payment date, amount, payee, and optional proof (e.g. e-wallet screenshot)
- Remaining balance is automatically updated upon each payment

### Installments
- Visual progress indicator showing payment completion percentage
- Add payments or skip terms per installment period
- Installment statuses: `NOT STARTED`, `UNPAID`, `PAID`, `SKIPPED`, `DELINQUENT`

### Group / Payment Allocation
- Split expenses equally, by percentage, or by custom amount per person
- Supports shared items between specific group members
- Per-person allocation statuses: `UNPAID`, `PARTIALLY PAID`, `PAID`

### People & Groups
- Manage contacts and groups used as borrowers or lenders
- Auto-generated Reference IDs based on borrower and lender initials

### Pages & Screens
- **Homepage** — Overview dashboard
- **All Payments Record** — Full list of all financial entries
- **People & Groups** — Contact and group management

---

## Data Model Highlights

| Field | Notes |
|---|---|
| ID | Auto-generated UUID |
| Transaction Type | `Straight Expense`, `Installment Expense`, `Group Expense` |
| Payment Status | `UNPAID`, `PARTIALLY PAID`, `PAID` |
| Amount Remaining | Auto-updated on each payment; defaults to amount borrowed |
| Reference ID | Generated from borrower + lender initials |
| Receipt / Proof | Optional BLOB (photo of receipt or e-wallet screenshot) |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | TypeScript / JavaScript (React or Angular) |
| Backend | Java, Spring Boot, Spring Data JPA |
| Database | PostgreSQL |

---

## Assumptions & Limitations

- No user authentication — single user assumed
- An entry **cannot** be both an installment type and have a group as the borrower simultaneously
- Payment frequency for installments supports **Monthly** (1st–28th) and **Weekly** (Sunday–Saturday)

---

## Notes

- `Notes` fields throughout the app support rich alphanumeric input for free-form annotations on entries, payments, and allocations
- Proof of loan and payment proof are stored as BLOBs and are optional on all relevant entries
