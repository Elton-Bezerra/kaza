# Asaas integration dossier (MVP)

## Account model

The intended model is one Asaas **subaccount per active condominium**. The platform account is the parent account. Kaza should not call `POST /v3/accounts` from the public landing form or before application approval. After approval, KazaConta creates the subaccount, receives its `id`, `walletId`, and `apiKey`, and stores the key immediately in a secrets manager. Kaza should accept condominiums identified by CPF or CNPJ; whether a particular Asaas account/product permits the submitted responsible party is a provider-eligibility check, not a reason to reject a condominium during Kaza's initial application.

Asaas requires configuration per subaccount after creation, notably webhooks, tax/invoice settings, registration data, customers, and payments. New API-created subaccounts can also be subject to the regulatory evaluation period (currently up to 10 distinct account holders, R$2,000 issued payments per subaccount, and up to 60 days), so production rollout needs an operational review with Asaas.

## Required API flow

1. Approve the Kaza onboarding application.
2. Create the condominium and units locally, validating that all ideal fractions sum exactly to `1.00000000`.
3. Create the Asaas subaccount with `POST /v3/accounts` using the platform `access_token`.
4. Store the returned subaccount `apiKey` and `walletId` securely; never log or return the key.
5. Configure a webhook for that subaccount and use a unique internal condominium identifier in metadata where supported.
6. For every active occupant, create and persist one Asaas customer with `POST /v3/customers`.
7. At the monthly close, create one charge per unit with `POST /v3/payments`, using that unit's selected `billingType` (`PIX`, `BOLETO`, or `CREDIT_CARD`), the unit customer ID, the fractioned amount, and the due date. `DEBIT_CARD` is not sent by this integration.
8. Retrieve the Pix QR-code payload or boleto URL/linha digitável and send it through the notification provider.
9. Consume payment webhooks asynchronously and make processing idempotent by event/payment ID. Do not rely on polling as the primary reconciliation mechanism.

The adapter in `AsaasClient` is deliberately small so the rest of the application does not depend on provider DTOs. Before production, add retries with idempotency protection, timeouts, structured provider error mapping, and secret-manager-backed key storage.

## Split and service-account economics

Payment Split uses the recipient `walletId` in the payment `split` array. The platform wallet and percentage/fixed amount must be configured from the commercial contract, not hard-coded. The current MVP demonstrates the provider shape with `asaas.platform-wallet-id` and a 10% example; replace this with a persisted, versioned fee policy before real charges. A single split represents platform SaaS revenue; additional recipients should be added only when the legal/settlement model is defined.

## Payables and payment approval

The MVP records an expense as `PENDING_APPROVAL` and requires the authenticated condominium administrator plus a separate six-digit PIN to approve it. The PIN is stored as a BCrypt hash and is never returned. This is application authorization, not an Asaas “approval” primitive. Paying a third-party boleto is a separate Asaas Banking/Payables capability and must be confirmed against the account's enabled products and exact endpoint contract in the developer portal before wiring settlement. Keep the expense approval and the provider payment as separate state transitions; require an idempotency key and persist provider receipts.

## Webhook security and operations

Use HTTPS, validate the Asaas webhook access token, return quickly after durable persistence, and process events asynchronously. The endpoint currently validates the token and is intentionally a boundary for adding an outbox/queue. Production must persist raw event IDs, reject duplicates, map payment events to internal charges, and expose dead-letter/replay operations.

## Developer-account checklist

- Create a sandbox parent account and API key.
- Confirm BaaS/subaccount eligibility for the platform account and the condominium responsible party's CPF/CNPJ.
- Create one sandbox subaccount and verify the one-time API-key response.
- Configure and test per-subaccount webhooks.
- Confirm Pix, boleto, customer, split, and payables products are enabled and obtain the exact payables endpoint/permissions.
- Define fee, refund, chargeback, settlement, retention, and reconciliation policies.
- Move API keys to a secret manager and enable audit logging before production.

Sources: [Creating subaccounts](https://docs.asaas.com/docs/creating-subaccounts), [Payments overview](https://docs.asaas.com/docs/payments-overview), [Asaas documentation index](https://docs.asaas.com/llms.txt). Documentation contents and limits should be rechecked after developer-account access because product availability is account-specific.
