export type Attribution = {
  landingPath: string;
  referrer: string | null;
  utmSource: string | null;
  utmMedium: string | null;
  utmCampaign: string | null;
  utmContent: string | null;
  utmTerm: string | null;
};

const MAX_VALUE_LENGTH = 180;

function normalizeValue(value: string | null): string | null {
  if (!value) {
    return null;
  }

  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }

  return trimmed.slice(0, MAX_VALUE_LENGTH);
}

function normalizeReferrer(rawReferrer: string): string | null {
  if (!rawReferrer) {
    return null;
  }

  try {
    const url = new URL(rawReferrer);
    return `${url.origin}${url.pathname}`.slice(0, MAX_VALUE_LENGTH);
  } catch {
    return normalizeValue(rawReferrer);
  }
}

export function collectAttribution(): Attribution {
  if (typeof window === 'undefined') {
    return {
      landingPath: '/',
      referrer: null,
      utmSource: null,
      utmMedium: null,
      utmCampaign: null,
      utmContent: null,
      utmTerm: null,
    };
  }

  const url = new URL(window.location.href);
  const params = url.searchParams;
  const referrer = normalizeReferrer(window.document.referrer);

  return {
    landingPath: `${url.pathname}${url.search}`.slice(0, MAX_VALUE_LENGTH),
    referrer,
    utmSource: normalizeValue(params.get('utm_source')),
    utmMedium: normalizeValue(params.get('utm_medium')),
    utmCampaign: normalizeValue(params.get('utm_campaign')),
    utmContent: normalizeValue(params.get('utm_content')),
    utmTerm: normalizeValue(params.get('utm_term')),
  };
}
