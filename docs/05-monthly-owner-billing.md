# Monthly owner billing

## User perspective

At month close, the administrator starts one billing run for a specific month. They do not enter the total manually. Kaza calculates it from approved expenses and the condominium's agreed subscription price.

Monthly billing is available only for an `ACTIVE` condominium with the KazaConta/provider setup ready. An onboarding draft or application under review cannot issue charges.

## API call

```http
POST /api/v1/condominiums/{condominiumId}/billing-runs
Authorization: ******
```

```json
{
  "period": "2026-08",
  "dueDate": "2026-08-10"
}
```

## Calculation

For August, Kaza sums expenses due in August with status `APPROVED` or `PAID`, then adds the condominium-specific subscription price for every unit.

Example:

```text
Expenses:                         R$ 1,000.00
Condominium price: R$ 10 × 10 units = R$ 100.00
Total collected from residents:   R$ 1,100.00
```

Each unit receives:

```text
(R$ 1,000 × unit ideal fraction) + R$ 10 subscription
```

The amount is rounded to cents. Any rounding remainder is assigned to the final unit so all unit charges add up exactly to the billing-run total.

## Asaas calls

For each unit:

1. If needed, create an Asaas customer with `POST /v3/customers`.
2. Create one payment with `POST /v3/payments`.
3. Use that unit's stored `billingType`.
4. Store the returned Asaas payment ID and status.

If the condominium API key is not configured, Kaza still stores the calculated charges as `PENDING_PROVIDER`; no fake payment is reported as created.

## Why it happens only once

The database enforces one billing run per `(condominium, period)`. Every charge belongs to that run, and `(billing_run, unit)` is also unique. A second request for August is rejected instead of creating duplicate resident charges.

The provider integration will additionally need idempotency keys for network retries so a provider request cannot create a duplicate payment when the response is lost.
