# Expense submission and approval

## User perspective

The administrator records a supplier expense, such as water, electricity, cleaning, or a repair. The expense is not immediately paid. It waits for explicit approval with the condominium's separate six-digit approval PIN.

This screen is available only after the onboarding application is approved and the user has an active `SINDICO` membership. An authenticated applicant under review cannot create or approve expenses.

## Create an expense

```http
POST /api/v1/condominiums/{condominiumId}/expenses
Authorization: Bearer <sindico-token>
```

```json
{
  "description": "Water bill - August",
  "amount": 142.50,
  "dueDate": "2026-08-15",
  "barcode": "00190500954014481606906809350314337370000000100"
}
```

Kaza stores the expense as `PENDING_APPROVAL`. The barcode is retained for the future provider-payables operation and audit trail.

## Approve an expense

```http
POST /api/v1/condominiums/{condominiumId}/expenses/{expenseId}/approve
Authorization: Bearer <sindico-token>
```

```json
{
  "pin": "123456"
}
```

Kaza verifies the condominium, expense ownership, PIN hash, and current status. A valid approval records the approver and timestamp and moves the expense to `APPROVED`.

## Important boundary

Approval authorizes the expense; it does not currently settle the supplier payment through Asaas. The intended future lifecycle is:

```text
PENDING_APPROVAL → APPROVED → PAYMENT_SUBMITTED → PAID / FAILED
```

The monthly owner billing flow is separate: approved expenses are included in that month's amount collected from residents. Provider-payables settlement must be connected only after the exact Asaas Banking/Payables product and endpoint are confirmed.
