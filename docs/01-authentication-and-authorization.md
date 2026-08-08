# Authentication and authorization

Kaza keeps admin authentication server-side.

## Admin OAuth flow

The frontend constructs the Keycloak authorize URL itself:

- `client_id`
- `redirect_uri`
- `state`
- `nonce`
- `code_challenge`
- `code_challenge_method=S256`

Before redirecting, the browser starts a short-lived server challenge transaction:

1. `GET /api/v1/admin/auth/csrf`
2. `POST /api/v1/admin/auth/challenge` with `state`, `nonce`, and `codeVerifier`
3. redirect to Keycloak
4. Keycloak returns to `GET /api/v1/admin/auth/callback`
5. Java validates the stored challenge, exchanges the code with the confidential client secret plus PKCE verifier, validates issuer/audience/nonce/role, and creates the secure session

Tokens never leave the server-side session store.

## Admin routes

- `GET /api/v1/admin/auth/login` — convenience redirect flow for server-rendered/admin entry points
- `GET /api/v1/admin/auth/csrf` — bootstrap CSRF token for browser POSTs
- `POST /api/v1/admin/auth/challenge` — store a short-lived login transaction
- `GET /api/v1/admin/auth/callback` — code exchange and session creation
- `GET /api/v1/admin/auth/session` — current admin session
- `POST /api/v1/admin/auth/logout` — local logout plus refresh-token revocation when available

## Security model

- admin APIs require the authenticated server session and `SUPER_ADMIN`
- browser-supplied role claims are ignored
- CSRF protection applies to browser POSTs
- the session cookie is `HttpOnly`, `Secure`, and `SameSite=Lax`
- access/refresh tokens are kept server-side only
- session expiry and token refresh happen on the server

## Keycloak

The local realm export includes a confidential `kaza-admin-bff` client with:

- Authorization Code flow
- PKCE `S256`
- no implicit flow
- no direct access grants
- exact local redirect URI
- brute-force protection enabled

Production deployments should keep exact redirect URIs, HTTPS, and secret-managed client credentials.
