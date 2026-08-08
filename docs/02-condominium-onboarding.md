# Assisted condominium onboarding

## Product boundary

The public landing page collects a lead. It does not create an active condominium and it does not grant Síndico permissions.

Kaza uses an assisted onboarding process because a person can request a condominium account without necessarily being authorized to manage that building. The application is reviewed before the condominium and its memberships become active.

```text
Landing lead
→ magic link
→ Keycloak account
→ application draft
→ documents and condominium details
→ manual review
→ Kaza activation
→ KazaConta/provider setup
→ resident invitations
```

## 1. Landing page

The first form is intentionally small:

```text
Name
Email
Phone
Role: Síndico / Morador / Outro
```

```http
POST /api/v1/onboarding/leads
Content-Type: application/json

{
  "name": "Maria da Silva",
  "email": "maria@example.com",
  "phone": "+55 11 99999-9999",
  "role": "SINDICO",
  "contactConsent": true,
  "marketingConsent": false,
  "analyticsConsent": true,
  "source": "web-landing",
  "attribution": {
    "landingPath": "/?utm_source=google",
    "utmSource": "google",
    "utmMedium": "cpc",
    "utmCampaign": "microcondominios",
    "referrer": "https://example.com/landing"
  }
}
```

The public frontend keeps the form minimal, adds explicit consent for contact/marketing, and sends anonymous funnel events only with consent. The current request and analytics boundary is documented in [`public-landing-contract.md`](public-landing-contract.md).

This creates an onboarding lead. The selected role is routing information, not authorization. A Morador is routed toward an invitation/access flow; a Síndico is routed toward condominium application; `Outro` can be handled manually.

The endpoint validates and bounds every field and requires contact consent. Repeated submissions for the same normalized email are limited to one every 15 minutes. Attribution is stored only with analytics consent. A lead never creates a condominium, membership, or authorization grant.

## 2. Magic link and account setup

Kaza sends a single-use, expiring email link. The link confirms email ownership and takes the applicant through Keycloak account setup. Keycloak owns password creation, login sessions, email verification, and optional MFA.

The `SUPER_ADMIN` can trigger this invitation from the admin portal after reviewing a lead. The backend stores only a hash of the token, marks it single-use, and sends the link through the existing email outbox.

The client then uses Authorization Code + PKCE with the `kaza-web` client. On the first authenticated request, Kaza provisions the local `users` record from the immutable Keycloak `sub`.

The applicant is authenticated, but does not yet have a `SINDICO` condominium membership.

## 3. Application draft

```http
POST /api/v1/onboarding/applications
Authorization: Bearer <keycloak-token>
```

The application collects:

- applicant identity and contact details;
- condominium name and address;
- responsible person's CPF or CNPJ, when available;
- number of units;
- proposed subscription price;
- initial unit and ideal-fraction data.

CPF/CNPJ is validated for format and check digit when supplied. A condominium may proceed without a CNPJ; provider eligibility is checked separately.

The application starts as:

```text
DRAFT
```

The applicant can save and resume it after signing in again.

## 4. Evidence upload

The applicant uploads evidence that they have a relationship with the building, such as:

- election or appointment minutes;
- owner authorization;
- condominium convention or registration document;
- management contract;
- proof of address or utility document;
- another document that supports the request.

For informal small condominiums, the reviewer may request a signed owner declaration or perform a phone/video confirmation.

```http
POST /api/v1/onboarding/applications/{id}/documents
Content-Type: multipart/form-data
```

Documents should use private object storage, signed download URLs, file-size/type validation, malware scanning, access auditing, encryption, and a retention policy. They should not be stored as unrestricted public files.

## 5. Submit for review

```http
POST /api/v1/onboarding/applications/{id}/submit
Authorization: Bearer <keycloak-token>
```

The status becomes:

```text
UNDER_REVIEW
```

The applicant cannot yet create active memberships or invite residents.

## 6. Manual review

The `SUPER_ADMIN` reviews:

- applicant identity and contact ownership;
- CPF/CNPJ consistency;
- condominium details;
- uploaded evidence;
- duplicate condominium risk;
- unit and ideal-fraction data.

The backend exposes protected review/list endpoints under `/api/v1/admin/onboarding/**`. Lead creation also queues two emails: an internal notification and a prospect confirmation. In local development they are captured by Mailpit.
Those review endpoints are enforced in the backend with `SUPER_ADMIN` authorization; the frontend does not participate in the access decision.

Possible decisions:

```http
POST /api/v1/admin/onboarding/applications/{id}/approve
POST /api/v1/admin/onboarding/applications/{id}/request-information
POST /api/v1/admin/onboarding/applications/{id}/reject
```

Application states:

```text
NOT_STARTED
→ DRAFT
→ UNDER_REVIEW
→ NEEDS_MORE_INFORMATION
→ APPROVED / REJECTED
```

## 7. Activation

After approval, Kaza:

1. creates the operational condominium;
2. creates the applicant's `SINDICO` membership;
3. creates units and validates ideal fractions sum to `1.00000000`;
4. records the agreed subscription price;
5. starts KazaConta/provider setup;
6. makes the condominium active only after required setup succeeds.

The applicant can now enter the Síndico dashboard.

```text
APPROVED
→ KAZA_ACTIVE
→ KAZACONTA_PENDING
→ ACTIVE
```

Kaza and KazaConta are separate product boundaries. Kaza owns condominium operations; KazaConta owns subaccounts, collections, splits, payment-provider integration, and future banking features.

## 8. Resident invitations

Residents should not need Keycloak accounts during Síndico application submission. The Síndico first creates or confirms units and occupants, then sends invitations:

```http
POST /api/v1/condominiums/{id}/units/{unitId}/occupant-invitations
```

The resident follows the invitation, creates or signs into Keycloak, and accepts:

```http
POST /api/v1/invitations/{token}/accept
Authorization: Bearer <resident-keycloak-token>
```

Kaza then links the Keycloak subject, creates the local `MORADOR` membership, and starts the occupant/service-account relationship.

## Implemented applicant API behavior

An application can optionally reference a lead. The lead email must then match the authenticated token email, and an applicant can have only one open application.

Drafts are saved with:

```http
PATCH /api/v1/onboarding/applications/{id}
Content-Type: application/json

{
  "responsibleName": "Maria da Silva",
  "responsibleEmail": "maria@example.com",
  "responsiblePhone": "+55 11 99999-9999",
  "taxId": "52998224725",
  "condominiumName": "Condomínio Flores",
  "addressLine": "Rua das Flores, 10",
  "addressCity": "São Paulo",
  "addressState": "SP",
  "postalCode": "01001000",
  "proposedUnitCount": 2,
  "subscriptionPricePerUnit": 7.00,
  "units": [
    {"identifier": "101", "idealFraction": 0.50000000},
    {"identifier": "102", "idealFraction": 0.50000000}
  ]
}
```

Applicants can read or change only their own `DRAFT` or `NEEDS_MORE_INFORMATION` application. During review, status remains visible through `GET /api/v1/me`, but applicant detail and document operations are closed.

The document API enforces ownership, at most 10 active documents, a 10 MB limit, PDF/JPEG/PNG allowlisting, content-signature checks, SHA-256 metadata, and deletion state. An `OnboardingDocumentStorage` boundary keeps bytes private; its initial adapter stores them in PostgreSQL and exposes no local path or public URL. Files remain `PENDING` until a production malware-scanner adapter is added.

```http
GET /api/v1/onboarding/applications/{id}/documents
DELETE /api/v1/onboarding/applications/{id}/documents/{documentId}
```

Submission requires contact and address data, a positive subscription price, at least one supporting document, a unit count matching the unit draft, valid CPF/CNPJ check digits when supplied, unique unit identifiers, and ideal fractions totaling exactly `1.00000000`.

Manual review and activation APIs are the next backend slice. The applicant slice persists the full state model but currently performs only the explicit `DRAFT`/`NEEDS_MORE_INFORMATION` → `UNDER_REVIEW` transition.

## Production security requirements

- Use HTTPS everywhere for the deployed auth and API stack.
- Configure exact redirect URIs and web origins for the deployed client.
- Keep `SUPER_ADMIN` accounts behind MFA and email verification.
- Use strong admin passwords and rotate them through a secret manager, not env-file plaintext.
- Keep brute-force protection enabled in Keycloak and avoid permissive CORS fallbacks.

## Why this design

```text
Keycloak identity
  = who the person is

Onboarding application
  = what the person is requesting

Review decision
  = whether Kaza accepts the request

Condominium membership
  = what the person is authorized to do
```

An authenticated applicant is therefore not automatically a Síndico. Authority begins only after the application is approved and the membership is created.
