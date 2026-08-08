# Kaza landing page

Frontend público da Kaza para captação de leads e apresentação do SaaS.

## Stack

- Next.js App Router
- TypeScript
- CSS puro com foco em SEO, acessibilidade e performance

## Ambiente

Copie `./.env.example` para `./.env.local` e ajuste:

- `NEXT_PUBLIC_SITE_URL`
- `NEXT_PUBLIC_KAZA_API_BASE_URL`
- `NEXT_PUBLIC_KAZA_ADMIN_BFF_BASE_URL`
- `NEXT_PUBLIC_KAZA_ADMIN_SESSION_ROUTE`
- `NEXT_PUBLIC_KAZA_ADMIN_AUTH_REGISTER_ROUTE`
- `NEXT_PUBLIC_KAZA_ADMIN_AUTH_CALLBACK_ROUTE`
- `NEXT_PUBLIC_KAZA_ADMIN_AUTH_LOGOUT_ROUTE`
- `NEXT_PUBLIC_KAZA_ADMIN_OAUTH_ISSUER_URL`
- `NEXT_PUBLIC_KAZA_ADMIN_OAUTH_CLIENT_ID`
- `NEXT_PUBLIC_KAZA_ADMIN_CALLBACK_PATH`
- `NEXT_PUBLIC_KAZA_ADMIN_POST_LOGIN_PATH`
- `NEXT_PUBLIC_KAZA_ADMIN_POST_LOGOUT_PATH`
- `NEXT_PUBLIC_KAZA_LEAD_PATH`
- `NEXT_PUBLIC_KAZA_ANALYTICS_PATH`

## Rodando localmente

```bash
cd web
npm install
npm run dev
```

Acesse `http://localhost:3000`.

## Build

```bash
npm run build
npm run start
```

## Contrato com o backend

- Leads: `POST /api/v1/onboarding/leads`
- Eventos anônimos do funil: `POST /api/v1/public/landing-events`
- Convites públicos de onboarding: `GET /api/v1/public/onboarding/invitations/{token}`
- Página pública do convite: `/invite/{token}`

Veja `../docs/public-landing-contract.md`.

## Admin interno

Rota: `http://localhost:3000/admin`

- Dashboard interno para contas autorizadas com resumo, filtros, listas e painel de detalhes.
- A área confirma sessão no BFF antes de renderizar dados.
- Se não houver sessão, o painel inicia o fluxo OAuth híbrido do frontend.
- O frontend cria `state`, `nonce` e PKCE `S256`, registra a transação curta no BFF e redireciona para o Keycloak.
- A callback passa por `/admin/auth/callback`, que faz a ponte para o callback do BFF em Java.
- As chamadas usam `credentials: 'include'` e rotas do BFF configuráveis.
- Não há token do navegador, PKCE persistido, localStorage ou sessionStorage de tokens; só a transação efêmera de estado fica em sessionStorage e é apagada no callback.
- O painel consome `GET /api/v1/admin/auth/session` e `GET /api/v1/admin/onboarding/*` via `src/lib/admin-api.ts`.
- O painel também dispara `POST /api/v1/admin/onboarding/leads/{id}/invitation` para enviar um convite público ao lead.
- Logout limpa a sessão do BFF.

Contrato esperado:

- `POST /api/v1/admin/auth/login` registra `state`, `nonce`, `codeVerifier`, `codeChallenge`, `redirectUri` e `postLoginRedirectUri`.
- `GET /admin/auth/callback` recebe o retorno do Keycloak e encaminha `code`/`state` ao callback do BFF.
- `GET /api/v1/admin/auth/callback` finaliza a troca no Java e define a sessão.
- `POST /api/v1/admin/auth/logout` encerra a sessão segura.

## Desenvolvimento

```bash
cd web
npm install
npm run dev
```

Depois abra:

- Landing page: `http://localhost:3000`
- Admin: `http://localhost:3000/admin`
