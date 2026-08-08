# Long-term integrations: DDA and automatic supplier payments

These capabilities are intentionally outside the current MVP.

They also cannot be triggered by a public onboarding application. The Síndico must first be verified, the condominium must be approved, and KazaConta must have an active provider account.

## What the MVP does

The MVP supports:

- manual supplier-expense entry;
- Síndico PIN approval;
- monthly owner billing;
- PIX, boleto, and credit-card owner charges;
- Asaas payment-status reconciliation boundary.

The MVP does **not** discover supplier bills automatically or settle approved supplier bills automatically.

## DDA

DDA (Débito Direto Autorizado) requires a banking/data-access integration that can retrieve bills addressed to the condominium's financial account. The future flow is:

```text
Bank/Asaas DDA source
→ authenticated synchronization
→ raw external bill event
→ duplicate detection
→ bill normalization
→ expense draft
→ Síndico review
→ approval
```

DDA must never jump directly from discovery to payment. Every imported bill should remain a draft until the Síndico verifies the beneficiary, amount, due date, and supporting data.

## Automatic supplier payments

Automatic payment is a separate provider capability from creating owner charges. It will require:

- an enabled Asaas Banking/Payables product or another banking provider;
- provider credentials and scopes for the condominium account;
- an exact endpoint and request contract;
- idempotency keys;
- a payment instruction/receipt identifier;
- asynchronous webhooks for submitted, paid, rejected, and reversed states;
- retry and dead-letter handling.

The intended state machine is:

```text
EXPENSE_DRAFT
→ PENDING_APPROVAL
→ APPROVED
→ PAYMENT_SUBMITTED
→ PAID
```

Failure paths must be explicit:

```text
APPROVED → PAYMENT_FAILED
PAID → REVERSED
```

The provider payment must be linked to the local expense by a durable external ID. Webhook events must be persisted before processing and deduplicated by provider event ID.

## Why this is deferred

DDA and supplier settlement depend on account-specific banking products, permissions, regulatory constraints, and provider contracts that are not guaranteed by the ordinary Asaas customer/payment API. Keeping them behind a provider adapter prevents the condominium ledger and approval workflow from depending on undocumented behavior.
