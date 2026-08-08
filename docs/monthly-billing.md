# Monthly billing model

Monthly billing is a backend-controlled **billing run**, not an endpoint that accepts an arbitrary total. The endpoint is:

```http
POST /api/v1/condominiums/{condominiumId}/billing-runs
{
  "period": "2026-08",
  "dueDate": "2026-08-10"
}
```

## Calculation

For the requested month, Kaza includes expenses whose due date falls in that month and whose status is `APPROVED` or `PAID`. It then adds the subscription price agreed for that condominium once for every unit. The price is stored on `condominiums.subscription_price_per_unit`, so condominium A can pay `R$ 10.00` per unit while condominium B pays `R$ 5.00`.

Example:

```text
Approved expenses in August: R$ 1,000.00
Subscription: 10 units × R$ 7.00: R$ 70.00
Total to collect: R$ 1,070.00
```

Each unit receives:

```text
(monthly expenses × ideal fraction) + subscription price per unit
```

All allocations are rounded to cents. Any rounding remainder is assigned to the final unit so the sum of unit charges exactly equals the billing-run total.

The subscription is a separate economic component from condominium expenses. Asaas split configuration can later route the subscription/platform share to the platform wallet; the local calculation remains auditable regardless of provider configuration.

## Payment method per unit

Each unit stores its own payment method. The supported values are:

```text
PIX
BOLETO
CREDIT_CARD
```

The administrator can update a unit's preference with:

```http
PUT /api/v1/condominiums/{condominiumId}/units/{unitId}/payment-method
{
  "billingType": "CREDIT_CARD"
}
```

The billing run uses that stored preference when creating the unit's Asaas payment. `DEBIT_CARD` is rejected because it is not accepted by the API integration.

## Idempotency

`billing_runs` has a unique constraint on `(condominium_id, period)`. A second request for the same condominium and month is rejected. Every generated `charge` belongs to exactly one billing run, so a unit cannot receive two charges for the same period.

The monthly run stores the calculated expense total, subscription total, overall total, status, due date on each charge, and the provider payment ID when Asaas is configured. If the condominium Asaas key is not configured yet, charges are persisted as `PENDING_PROVIDER`; this allows calculation and review before credentials are added without pretending that money was collected.

## Lifecycle

```text
Expenses entered
→ Expense approved with PIN
→ Monthly billing run created
→ One charge generated per unit
→ Asaas payment created (when configured)
→ Asaas webhook reconciles payment status
```

Expense approval and owner billing are intentionally separate: approving a supplier expense authorizes money out; the billing run creates money-in receivables. A future provider-payables integration should transition expenses through `PAYMENT_SUBMITTED`, `PAID`, or `FAILED` independently of owner-charge reconciliation.

## Pricing changes

The agreed price is provided during condominium onboarding and persisted on the condominium. Existing billing runs retain their calculated subscription total. A future contract/pricing table should version changes by effective date rather than modifying the current condominium value without an audit trail.
