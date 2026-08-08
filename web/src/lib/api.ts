import { LEAD_PATH, resolveApiUrl } from '@/lib/site';

export type LeadPayload = {
  name: string;
  email: string;
  phone: string;
  role: 'SINDICO' | 'MORADOR' | 'OUTRO';
  contactConsent: boolean;
  marketingConsent: boolean;
  analyticsConsent: boolean;
  attribution: {
    landingPath: string;
    referrer: string | null;
    utmSource: string | null;
    utmMedium: string | null;
    utmCampaign: string | null;
    utmContent: string | null;
    utmTerm: string | null;
  };
  source: 'web-landing';
};

export async function submitLead(payload: LeadPayload): Promise<Response> {
  return fetch(resolveApiUrl(LEAD_PATH), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });
}
