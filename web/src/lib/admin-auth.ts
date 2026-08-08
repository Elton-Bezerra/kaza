const DEFAULT_ADMIN_BFF_BASE_URL = 'http://localhost:8080';
const DEFAULT_ADMIN_SESSION_ROUTE = '/api/v1/admin/auth/session';
const DEFAULT_ADMIN_AUTH_REGISTER_ROUTE = '/api/v1/admin/auth/challenge';
const DEFAULT_ADMIN_CSRF_ROUTE = '/api/v1/admin/auth/csrf';
const DEFAULT_ADMIN_AUTH_CALLBACK_ROUTE = '/api/v1/admin/auth/callback';
const DEFAULT_ADMIN_AUTH_LOGOUT_ROUTE = '/api/v1/admin/auth/logout';
const DEFAULT_ADMIN_OAUTH_ISSUER_URL = 'http://localhost:8081/realms/kaza';
const DEFAULT_ADMIN_OAUTH_CLIENT_ID = 'kaza-admin-bff';
const DEFAULT_ADMIN_CALLBACK_PATH = '/admin/auth/callback';
const DEFAULT_ADMIN_POST_LOGIN_PATH = '/admin';
const DEFAULT_ADMIN_POST_LOGOUT_PATH = '/admin';

const ADMIN_AUTH_TRANSACTION_STORAGE_KEY = 'kaza.admin.auth.transaction.v1';
const CODE_CHALLENGE_METHOD = 'S256' as const;

export type AdminAuthEnvironment = {
  bffBaseUrl: string;
  csrfRoute: string;
  sessionRoute: string;
  authRegisterRoute: string;
  authCallbackRoute: string;
  authLogoutRoute: string;
  oauthIssuerUrl: string;
  oauthClientId: string;
  callbackPath: string;
  postLoginPath: string;
  postLogoutPath: string;
};

export type AdminAuthTransaction = {
  state: string;
  nonce: string;
  callbackPath: string;
  postLoginPath: string;
  createdAt: string;
};

export type AdminAuthRegistrationPayload = {
  state: string;
  nonce: string;
  codeVerifier: string;
  codeChallenge: string;
  codeChallengeMethod: typeof CODE_CHALLENGE_METHOD;
  redirectUri: string;
  postLoginRedirectUri: string;
};

export type AdminAuthCallbackPayload = {
  code: string;
  state: string;
};

export type AdminAuthStartResult = {
  authorizationUrl: string;
  transaction: AdminAuthTransaction;
};

function normalizeUrl(value: string): string {
  return value.replace(/\/+$/, '');
}

function normalizePath(value: string): string {
  const trimmed = value.trim();

  if (!trimmed || trimmed === '/') {
    return '';
  }

  return trimmed.replace(/\/+$/, '').replace(/^\/+/, '/');
}

function resolveUrl(baseUrl: string, route: string): string {
  const normalizedRoute = normalizePath(route) || '/';
  const normalizedBaseUrl = normalizeUrl(baseUrl);

  if (!normalizedBaseUrl) {
    return normalizedRoute;
  }

  return new URL(normalizedRoute, `${normalizedBaseUrl}/`).toString();
}

function resolveBrowserUrl(pathOrUrl: string): string {
  if (/^https?:\/\//i.test(pathOrUrl)) {
    return pathOrUrl;
  }

  const baseUrl = typeof window !== 'undefined' ? window.location.origin : process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000';
  return new URL(pathOrUrl, `${normalizeUrl(baseUrl)}/`).toString();
}

function getSiteUrl(path: string): string {
  return new URL(path, `${normalizeUrl(process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3000')}/`).toString();
}

function readStoredTransaction(): AdminAuthTransaction | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const raw = window.sessionStorage.getItem(ADMIN_AUTH_TRANSACTION_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as Partial<AdminAuthTransaction>;
    if (
      typeof parsed.state !== 'string' ||
      typeof parsed.nonce !== 'string' ||
      typeof parsed.callbackPath !== 'string' ||
      typeof parsed.postLoginPath !== 'string' ||
      typeof parsed.createdAt !== 'string'
    ) {
      return null;
    }

    return parsed as AdminAuthTransaction;
  } catch {
    return null;
  }
}

function writeStoredTransaction(transaction: AdminAuthTransaction): void {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.sessionStorage.setItem(ADMIN_AUTH_TRANSACTION_STORAGE_KEY, JSON.stringify(transaction));
  } catch {
    // Ignore storage failures.
  }
}

export function clearAdminAuthTransaction(): void {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    window.sessionStorage.removeItem(ADMIN_AUTH_TRANSACTION_STORAGE_KEY);
  } catch {
    // Ignore storage failures.
  }
}

export function getStoredAdminAuthTransaction(): AdminAuthTransaction | null {
  return readStoredTransaction();
}

export function getAdminAuthEnvironment(): AdminAuthEnvironment {
  const bffBaseUrl = normalizeUrl(process.env.NEXT_PUBLIC_KAZA_ADMIN_BFF_BASE_URL ?? DEFAULT_ADMIN_BFF_BASE_URL);

  return {
    bffBaseUrl,
    csrfRoute: resolveUrl(bffBaseUrl, DEFAULT_ADMIN_CSRF_ROUTE),
    sessionRoute: resolveUrl(bffBaseUrl, process.env.NEXT_PUBLIC_KAZA_ADMIN_SESSION_ROUTE ?? DEFAULT_ADMIN_SESSION_ROUTE),
    authRegisterRoute: resolveUrl(
      bffBaseUrl,
      process.env.NEXT_PUBLIC_KAZA_ADMIN_AUTH_REGISTER_ROUTE ?? DEFAULT_ADMIN_AUTH_REGISTER_ROUTE,
    ),
    authCallbackRoute: resolveUrl(
      bffBaseUrl,
      process.env.NEXT_PUBLIC_KAZA_ADMIN_AUTH_CALLBACK_ROUTE ?? DEFAULT_ADMIN_AUTH_CALLBACK_ROUTE,
    ),
    authLogoutRoute: resolveUrl(
      bffBaseUrl,
      process.env.NEXT_PUBLIC_KAZA_ADMIN_AUTH_LOGOUT_ROUTE ?? DEFAULT_ADMIN_AUTH_LOGOUT_ROUTE,
    ),
    oauthIssuerUrl: normalizeUrl(process.env.NEXT_PUBLIC_KAZA_ADMIN_OAUTH_ISSUER_URL ?? DEFAULT_ADMIN_OAUTH_ISSUER_URL),
    oauthClientId: process.env.NEXT_PUBLIC_KAZA_ADMIN_OAUTH_CLIENT_ID ?? DEFAULT_ADMIN_OAUTH_CLIENT_ID,
    callbackPath: normalizePath(process.env.NEXT_PUBLIC_KAZA_ADMIN_CALLBACK_PATH ?? DEFAULT_ADMIN_CALLBACK_PATH) || '/admin/auth/callback',
    postLoginPath: normalizePath(process.env.NEXT_PUBLIC_KAZA_ADMIN_POST_LOGIN_PATH ?? DEFAULT_ADMIN_POST_LOGIN_PATH) || '/admin',
    postLogoutPath: normalizePath(process.env.NEXT_PUBLIC_KAZA_ADMIN_POST_LOGOUT_PATH ?? DEFAULT_ADMIN_POST_LOGOUT_PATH) || '/admin',
  };
}

export function resolveAdminRoute(route: string): string {
  const environment = getAdminAuthEnvironment();
  return resolveUrl(environment.bffBaseUrl, route);
}

function base64UrlEncode(value: Uint8Array): string {
  let binary = '';
  value.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });

  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function randomString(byteLength: number): string {
  const buffer = new Uint8Array(byteLength);
  const webCrypto = globalThis.crypto;

  if (!webCrypto) {
    throw new Error('O navegador não oferece suporte a crypto.');
  }

  webCrypto.getRandomValues(buffer);
  return base64UrlEncode(buffer);
}

async function createCodeChallenge(codeVerifier: string): Promise<string> {
  const webCrypto = globalThis.crypto;
  if (!webCrypto?.subtle) {
    throw new Error('O navegador não oferece suporte a PKCE.');
  }

  const digest = await webCrypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier));
  return base64UrlEncode(new Uint8Array(digest));
}

function buildAuthorizeUrl(environment: AdminAuthEnvironment, params: AdminAuthRegistrationPayload): string {
  const authorizeUrl = new URL('protocol/openid-connect/auth', `${normalizeUrl(environment.oauthIssuerUrl)}/`);
  authorizeUrl.searchParams.set('client_id', environment.oauthClientId);
  authorizeUrl.searchParams.set('response_type', 'code');
  authorizeUrl.searchParams.set('scope', 'openid profile email');
  authorizeUrl.searchParams.set('redirect_uri', params.redirectUri);
  authorizeUrl.searchParams.set('state', params.state);
  authorizeUrl.searchParams.set('nonce', params.nonce);
  authorizeUrl.searchParams.set('code_challenge', params.codeChallenge);
  authorizeUrl.searchParams.set('code_challenge_method', params.codeChallengeMethod);

  return authorizeUrl.toString();
}

async function postJson<T>(url: string, body: unknown, csrfToken?: string): Promise<T | null> {
  const response = await fetch(url, {
    method: 'POST',
    cache: 'no-store',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(csrfToken ? { 'X-CSRF-TOKEN': csrfToken } : {}),
    },
    body: JSON.stringify(body),
  });

  const text = await response.text();

  if (!response.ok) {
    let detail: string | null = null;

    try {
      const payload = JSON.parse(text) as { message?: string; error?: string; detail?: string };
      detail = payload.message ?? payload.error ?? payload.detail ?? null;
    } catch {
      detail = text.trim() || null;
    }

    throw new Error(detail || `Falha na requisição administrativa (${response.status}).`);
  }

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}

async function fetchCsrfToken(environment: AdminAuthEnvironment): Promise<string> {
  const response = await fetch(environment.csrfRoute, {
    cache: 'no-store',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`Falha ao preparar a autenticação administrativa (${response.status}).`);
  }
  const payload = (await response.json()) as { token?: string };
  if (!payload.token) {
    throw new Error('Não foi possível preparar a autenticação administrativa.');
  }
  return payload.token;
}

export async function startAdminAuthFlow(): Promise<void> {
  const environment = getAdminAuthEnvironment();
  const codeVerifier = randomString(64);
  const codeChallenge = await createCodeChallenge(codeVerifier);
  const state = randomString(32);
  const nonce = randomString(32);
  const redirectUri = getSiteUrl(environment.callbackPath);
  const postLoginRedirectUri = getSiteUrl(environment.postLoginPath);
  const transaction: AdminAuthTransaction = {
    state,
    nonce,
    callbackPath: environment.callbackPath,
    postLoginPath: environment.postLoginPath,
    createdAt: new Date().toISOString(),
  };

  writeStoredTransaction(transaction);

  const registrationPayload: AdminAuthRegistrationPayload = {
    state,
    nonce,
    codeVerifier,
    codeChallenge,
    codeChallengeMethod: CODE_CHALLENGE_METHOD,
    redirectUri,
    postLoginRedirectUri,
  };

  try {
    const csrfToken = await fetchCsrfToken(environment);
    const response = await postJson<{ authorizationUrl?: string; authorizeUrl?: string }>(
      environment.authRegisterRoute,
      registrationPayload,
      csrfToken,
    );

    const authorizationUrl =
      response?.authorizationUrl ?? response?.authorizeUrl ?? buildAuthorizeUrl(environment, registrationPayload);
    window.location.assign(authorizationUrl);
  } catch (error) {
    clearAdminAuthTransaction();
    throw error;
  }
}

export async function completeAdminAuthCallback(code: string, state: string): Promise<void> {
  const environment = getAdminAuthEnvironment();
  const transaction = readStoredTransaction();

  if (!transaction) {
    throw new Error('A transação de autenticação expirou.');
  }

  if (transaction.state !== state) {
    clearAdminAuthTransaction();
    throw new Error('O parâmetro state da autenticação não confere.');
  }

  clearAdminAuthTransaction();
  const callbackUrl = new URL(resolveBrowserUrl(environment.authCallbackRoute));
  callbackUrl.searchParams.set('code', code);
  callbackUrl.searchParams.set('state', state);
  callbackUrl.searchParams.set('redirect_uri', getSiteUrl(transaction.callbackPath));
  callbackUrl.searchParams.set('post_login_redirect_uri', getSiteUrl(transaction.postLoginPath));

  window.location.replace(callbackUrl.toString());
}

export async function performAdminLogout(): Promise<void> {
  const environment = getAdminAuthEnvironment();
  clearAdminAuthTransaction();

  try {
    await postJson(environment.authLogoutRoute, {});
  } catch {
    // Ignore logout transport errors; the local session view will still be cleared.
  } finally {
    window.location.replace(getSiteUrl(environment.postLogoutPath));
  }
}
