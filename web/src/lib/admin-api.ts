import { resolveAdminRoute } from '@/lib/admin-auth';

export const ADMIN_SESSION_ROUTE = '/api/v1/admin/auth/session';
export const ADMIN_SUMMARY_ROUTE = '/api/v1/admin/onboarding/summary';
export const ADMIN_LEADS_ROUTE = '/api/v1/admin/onboarding/leads';
export const ADMIN_LEAD_DETAIL_ROUTE = (id: string) => `/api/v1/admin/onboarding/leads/${encodeURIComponent(id)}`;
export const ADMIN_LEAD_INVITATION_ROUTE = (id: string) =>
  `/api/v1/admin/onboarding/leads/${encodeURIComponent(id)}/invitation`;
export const ADMIN_APPLICATIONS_ROUTE = '/api/v1/admin/onboarding/applications';
export const ADMIN_APPLICATION_DETAIL_ROUTE = (id: string) =>
  `/api/v1/admin/onboarding/applications/${encodeURIComponent(id)}`;

export type AdminSessionResponse = {
  authenticated?: boolean;
  roles?: string[];
  name?: string;
  email?: string;
};

export type AdminAttribution = {
  landingPath: string | null;
  referrer: string | null;
  utmSource: string | null;
  utmMedium: string | null;
  utmCampaign: string | null;
  utmContent: string | null;
  utmTerm: string | null;
};

export type AdminSummary = {
  totalLeads: number;
  newLeads: number;
  totalApplications: number;
  pendingApplications: number;
  leadsByStatus: Array<{ status: string; count: number }>;
  applicationsByStatus: Array<{ status: string; count: number }>;
  leadSources: Array<{ source: string; count: number }>;
  applicationSources: Array<{ source: string; count: number }>;
  generatedAt: string | null;
};

export type AdminLeadRecord = {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  status: string;
  source: string | null;
  createdAt: string;
  updatedAt: string | null;
  contactedAt: string | null;
  assignedTo: string | null;
  notes: string | null;
  detailUrl: string | null;
  attribution: AdminAttribution | null;
  invitations: AdminLeadInvitationRecord[];
};

export type AdminLeadInvitationRecord = {
  id: string;
  status: string;
  applicationId: string | null;
  expiresAt: string;
  acceptedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminApplicationRecord = {
  id: string;
  applicantName: string;
  applicantEmail: string | null;
  applicantPhone: string | null;
  condominiumName: string | null;
  unitCount: number | null;
  status: string;
  stage: string | null;
  source: string | null;
  createdAt: string;
  updatedAt: string | null;
  submittedAt: string | null;
  decisionAt: string | null;
  assignedTo: string | null;
  notes: string | null;
  detailUrl: string | null;
  attribution: AdminAttribution | null;
};

export type AdminListResponse<T> = {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
};

type SpringPageResponse<T> = {
  content?: T[];
  totalElements?: number;
  number?: number;
  size?: number;
};

export type AdminListQuery = {
  q?: string;
  status?: string;
  source?: string;
  page?: number;
  pageSize?: number;
};

export class AdminApiError extends Error {
  status: number;
  endpoint: string;
  detail: string | null;

  constructor(status: number, endpoint: string, detail: string | null) {
    super(detail ?? `Admin API request failed with status ${status}.`);
    this.name = 'AdminApiError';
    this.status = status;
    this.endpoint = endpoint;
    this.detail = detail;
  }
}

function toQueryString(params: AdminListQuery | undefined): string {
  const searchParams = new URLSearchParams();

  if (!params) {
    return searchParams.toString();
  }

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }

    searchParams.set(key, String(value));
  });

  return searchParams.toString();
}

function resolveAdminUrl(path: string): string {
  return resolveAdminRoute(path);
}

function parseErrorDetail(responseText: string): string | null {
  if (!responseText) {
    return null;
  }

  try {
    const payload = JSON.parse(responseText) as { message?: string; error?: string; detail?: string };
    return payload.message ?? payload.error ?? payload.detail ?? null;
  } catch {
    return responseText.trim() || null;
  }
}

async function adminResponse(path: string, init?: RequestInit): Promise<Response> {
  const headers = new Headers(init?.headers);
  headers.set('Accept', 'application/json');
  const method = (init?.method ?? 'GET').toUpperCase();
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrfResponse = await fetch(resolveAdminRoute('/api/v1/admin/auth/csrf'), {
      cache: 'no-store',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    });
    if (!csrfResponse.ok) {
      throw new AdminApiError(csrfResponse.status, '/api/v1/admin/auth/csrf', 'Não foi possível validar a sessão administrativa.');
    }
    const csrfPayload = (await csrfResponse.json()) as { token?: string };
    if (!csrfPayload.token) {
      throw new AdminApiError(403, '/api/v1/admin/auth/csrf', 'Token CSRF ausente.');
    }
    headers.set('X-CSRF-TOKEN', csrfPayload.token);
  }

  const response = await fetch(resolveAdminUrl(path), {
    cache: 'no-store',
    credentials: 'include',
    ...init,
    headers,
  });

  if (!response.ok) {
    const responseText = await response.text();
    throw new AdminApiError(response.status, path, parseErrorDetail(responseText));
  }

  return response;
}

async function adminJsonRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await adminResponse(path, init);
  const responseText = await response.text();

  if (!responseText) {
    return null as T;
  }

  try {
    return JSON.parse(responseText) as T;
  } catch {
    throw new AdminApiError(response.status, path, 'A resposta administrativa retornou um JSON inválido.');
  }
}

async function adminPageRequest<T>(path: string, init?: RequestInit): Promise<AdminListResponse<T>> {
  const payload = await adminJsonRequest<AdminListResponse<T> & SpringPageResponse<T>>(path, init);
  if (Array.isArray(payload.items)) {
    return payload;
  }
  return {
    items: payload.content ?? [],
    total: payload.totalElements ?? 0,
    page: payload.number ?? 0,
    pageSize: payload.size ?? payload.content?.length ?? 0,
  };
}

export async function fetchAdminSession(signal?: AbortSignal): Promise<AdminSessionResponse | null> {
  const response = await adminResponse(ADMIN_SESSION_ROUTE, {
    signal,
  });
  const responseText = await response.text();

  if (!responseText) {
    return null;
  }

  try {
    return JSON.parse(responseText) as AdminSessionResponse;
  } catch {
    throw new AdminApiError(response.status, ADMIN_SESSION_ROUTE, 'A resposta da sessão administrativa é inválida.');
  }
}

export async function fetchAdminSummary(signal?: AbortSignal): Promise<AdminSummary> {
  return adminJsonRequest<AdminSummary>(ADMIN_SUMMARY_ROUTE, {
    signal,
  });
}

export async function fetchAdminLeads(
  query?: AdminListQuery,
  signal?: AbortSignal,
): Promise<AdminListResponse<AdminLeadRecord>> {
  const queryString = toQueryString(query);

  const page = await adminPageRequest<AdminLeadRecord & {
    declaredRole?: string;
    landingPath?: string | null;
    referrer?: string | null;
    utmSource?: string | null;
    utmMedium?: string | null;
    utmCampaign?: string | null;
    utmTerm?: string | null;
    utmContent?: string | null;
  }>(
    queryString ? `${ADMIN_LEADS_ROUTE}?${queryString}` : ADMIN_LEADS_ROUTE,
    {
      signal,
    },
  );
  return {
    ...page,
    items: page.items.map((lead) => ({
      ...lead,
      status: lead.status ?? 'NEW',
      updatedAt: lead.updatedAt ?? null,
      contactedAt: lead.contactedAt ?? null,
      assignedTo: lead.assignedTo ?? null,
      notes: lead.notes ?? null,
      detailUrl: lead.detailUrl ?? null,
      attribution: lead.attribution ?? {
        landingPath: lead.landingPath ?? null,
        referrer: lead.referrer ?? null,
        utmSource: lead.utmSource ?? null,
        utmMedium: lead.utmMedium ?? null,
        utmCampaign: lead.utmCampaign ?? null,
        utmContent: lead.utmContent ?? null,
        utmTerm: lead.utmTerm ?? null,
      },
      invitations: lead.invitations ?? [],
    })),
  };
}

export async function fetchAdminLead(id: string, signal?: AbortSignal): Promise<AdminLeadRecord> {
  const lead = await adminJsonRequest<AdminLeadRecord>(ADMIN_LEAD_DETAIL_ROUTE(id), {
    signal,
  });
  return {
    ...lead,
    invitations: lead.invitations ?? [],
    attribution: lead.attribution ?? null,
  };
}

export async function inviteAdminLead(id: string, signal?: AbortSignal): Promise<AdminLeadInvitationRecord> {
  return adminJsonRequest<AdminLeadInvitationRecord>(ADMIN_LEAD_INVITATION_ROUTE(id), {
    method: 'POST',
    signal,
  });
}

export async function fetchAdminApplications(
  query?: AdminListQuery,
  signal?: AbortSignal,
): Promise<AdminListResponse<AdminApplicationRecord>> {
  const queryString = toQueryString(query);

  const page = await adminPageRequest<AdminApplicationRecord & { applicantSubject?: string }>(
    queryString ? `${ADMIN_APPLICATIONS_ROUTE}?${queryString}` : ADMIN_APPLICATIONS_ROUTE,
    {
      signal,
    },
  );
  return {
    ...page,
    items: page.items.map((application) => ({
      ...application,
      applicantName: application.applicantName ?? application.applicantSubject ?? '',
      applicantPhone: application.applicantPhone ?? null,
      stage: application.stage ?? null,
      source: application.source ?? null,
      decisionAt: application.decisionAt ?? null,
      assignedTo: application.assignedTo ?? null,
      notes: application.notes ?? null,
      detailUrl: application.detailUrl ?? null,
      attribution: application.attribution ?? null,
    })),
  };
}

export async function fetchAdminApplication(id: string, signal?: AbortSignal): Promise<AdminApplicationRecord> {
  return adminJsonRequest<AdminApplicationRecord>(ADMIN_APPLICATION_DETAIL_ROUTE(id), {
    signal,
  });
}
