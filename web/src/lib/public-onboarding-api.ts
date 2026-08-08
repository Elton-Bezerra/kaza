export type ApplicationInvitationResponse = {
  id: string;
  leadId: string;
  leadName: string;
  leadEmail: string;
  status: string;
  expiresAt: string;
  acceptedAt: string | null;
  applicationId: string | null;
};

export type PublicApplicationResponse = {
  id: string;
  status: string;
  responsibleName: string | null;
  responsibleEmail: string | null;
  responsiblePhone: string | null;
  taxId: string | null;
  condominiumName: string | null;
  addressLine: string | null;
  addressCity: string | null;
  addressState: string | null;
  postalCode: string | null;
  proposedUnitCount: number | null;
  subscriptionPricePerUnit: string | number | null;
  units: Array<{ identifier: string; idealFraction: string | number }>;
  reviewReason: string | null;
  submittedAt: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type PublicDocumentResponse = {
  id: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  scanStatus: string;
  uploadedAt: string;
};

const BASE_PATH = '/api/v1/public/onboarding';

function invitationPath(token: string): string {
  return `${BASE_PATH}/invitations/${encodeURIComponent(token)}`;
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(resolveApiUrl(path), {
    cache: 'no-store',
    credentials: 'include',
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(init?.headers ?? {}),
    },
  });

  const text = await response.text();
  if (!response.ok) {
    let message = `Falha na requisição pública (${response.status}).`;
    try {
      const payload = JSON.parse(text) as { message?: string; error?: string; detail?: string };
      message = payload.message ?? payload.error ?? payload.detail ?? message;
    } catch {
      if (text.trim()) {
        message = text.trim();
      }
    }
    throw new Error(message);
  }

  if (!text) {
    return null as T;
  }

  return JSON.parse(text) as T;
}

export async function fetchInvitation(token: string): Promise<ApplicationInvitationResponse> {
  return requestJson<ApplicationInvitationResponse>(invitationPath(token));
}

export async function acceptInvitation(token: string): Promise<PublicApplicationResponse> {
  return requestJson<PublicApplicationResponse>(`${invitationPath(token)}/accept`, {
    method: 'POST',
  });
}

export async function fetchCurrentApplication(): Promise<PublicApplicationResponse> {
  return requestJson<PublicApplicationResponse>(`${BASE_PATH}/application`);
}

export async function updateCurrentApplication(payload: unknown): Promise<PublicApplicationResponse> {
  return requestJson<PublicApplicationResponse>(`${BASE_PATH}/application`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function submitCurrentApplication(): Promise<PublicApplicationResponse> {
  return requestJson<PublicApplicationResponse>(`${BASE_PATH}/application/submit`, {
    method: 'POST',
  });
}

export async function listCurrentDocuments(): Promise<PublicDocumentResponse[]> {
  return requestJson<PublicDocumentResponse[]>(`${BASE_PATH}/application/documents`);
}

export async function uploadCurrentDocument(file: File): Promise<PublicDocumentResponse> {
  const formData = new FormData();
  formData.set('file', file);
  return requestJson<PublicDocumentResponse>(`${BASE_PATH}/application/documents`, {
    method: 'POST',
    body: formData,
  });
}

export async function deleteCurrentDocument(documentId: string): Promise<void> {
  await requestJson<null>(`${BASE_PATH}/application/documents/${encodeURIComponent(documentId)}`, {
    method: 'DELETE',
  });
}
import { resolveApiUrl } from '@/lib/site';
