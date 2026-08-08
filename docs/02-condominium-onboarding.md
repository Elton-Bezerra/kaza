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
```

This creates an onboarding lead. The selected role is routing information, not authorization. A Morador is routed toward an invitation/access flow; a Síndico is routed toward condominium application; `Outro` can be handled manually.

## 2. Magic link and account setup

Kaza sends a single-use, expiring email link. The link confirms email ownership and takes the applicant through Keycloak account setup. Keycloak owns password creation, login sessions, email verification, and optional MFA.

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

CPF/CNPJ is validated for format, check digit, and duplicate use where applicable. A condominium may proceed without a CNPJ; provider eligibility is checked separately.

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
