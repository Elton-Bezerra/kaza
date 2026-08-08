# Authentication and authorization

## User perspective

Users sign in through Keycloak. Kaza does not manage passwords or issue its own access tokens. After sign-in, the client sends the Keycloak access token in every request:

```http
Authorization: ******
```

The token must contain one of the configured realm roles:

| Role | Purpose |
| --- | --- |
| `SUPER_ADMIN` | Platform-wide operations |
| `SINDICO` | Manage a condominium, expenses, payment preferences, and billing |
| `MORADOR` | Read permitted condominium information and charges |

## What happens

Spring Security validates the token signature using Keycloak's issuer configuration. The application extracts the subject (`sub`) and roles, then provisions a local `User` record when that subject is first seen. Condominium-management operations verify an active `CondominiumMembership`, so the same person can belong to multiple condominiums with different roles.

Membership is Kaza domain data: Keycloak authenticates the person, while Kaza decides which condominium context and role that person may use.

After sign-in, the client should call a Kaza session/bootstrap endpoint such as:

```http
GET /api/v1/me
Authorization: Bearer <keycloak-token>
```

The response should include the local user, active condominium memberships, and onboarding status. This is how the app decides whether to show:

```text
Start application
Continue draft
Application under review
Open Síndico dashboard
Accept resident invitation
```

An onboarding applicant is a Kaza application state, not a Keycloak role.

## Relevant API behavior

Unauthenticated requests receive `401 Unauthorized`. Authenticated users without the required role receive `403 Forbidden`. Webhook requests are not authenticated with Keycloak; they use the configured Asaas webhook token instead.

Key configuration:

```yaml
KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/kaza
```

## Local client configuration

The repository declares the local Keycloak realm as code in:

```text
infra/keycloak/kaza-realm.json
```

Compose imports it automatically with:

```yaml
command: start-dev --import-realm
```

The imported `kaza-web` client is a public client configured for:

- Authorization Code flow;
- PKCE with `S256`;
- no implicit flow;
- no password/direct-access grant;
- no client secret.

The web or mobile application should redirect users to Keycloak's authorization endpoint, then exchange the authorization code using PKCE:

```text
GET /realms/kaza/protocol/openid-connect/auth
  ?client_id=kaza-web
  &response_type=code
  &redirect_uri=http://localhost:5173/callback
  &scope=openid profile email
  &code_challenge=<base64url-sha256>
  &code_challenge_method=S256
```

After the callback, the client exchanges the code at:

```text
POST /realms/kaza/protocol/openid-connect/token
```

with `grant_type=authorization_code`, the same `redirect_uri`, and the generated `code_verifier`. The client then sends the returned access token to Kaza.
