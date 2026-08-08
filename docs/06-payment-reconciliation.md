# Payment reconciliation

## User perspective

After a resident pays a PIX, boleto, or credit-card charge, the charge should move from its initial Asaas status to a settled status in Kaza. The administrator and resident can then rely on the ledger rather than manually checking the provider.

Reconciliation starts only after KazaConta has created the provider account and payment. It is not part of the public onboarding application itself.

## Provider callback

Asaas sends an event to:

```http
POST /api/v1/webhooks/asaas
asaas-access-token: <configured-webhook-token>
```

The endpoint validates the shared webhook token and accepts the event boundary. The production implementation must then durably store the event, enqueue processing, and return quickly.

## Intended processing

1. Validate the webhook token.
2. Persist the provider event ID and reject duplicates.
3. Find the local charge by Asaas payment ID.
4. Map the Asaas status to the local charge status.
5. Update the billing run's reconciliation state.
6. Make the result available in the resident and administrator views.

Typical payment events include payment creation, confirmation/receipt, overdue, cancellation, and failure. Webhooks are preferred over polling because they reduce reconciliation delay and provider traffic.

The current MVP has the authenticated boundary but not the durable event/outbox processor yet. Until that worker exists, provider statuses must not be treated as fully reconciled.
