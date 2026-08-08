export type ConsentPreferences = {
  analytics: boolean;
  marketing: boolean;
  updatedAt: string;
};

const CONSENT_STORAGE_KEY = 'kaza.landing.consent.v1';

export const DEFAULT_CONSENT: ConsentPreferences = {
  analytics: false,
  marketing: false,
  updatedAt: '',
};

export function readConsent(): ConsentPreferences | null {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    const raw = window.localStorage.getItem(CONSENT_STORAGE_KEY);
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw) as Partial<ConsentPreferences>;
    return {
      analytics: Boolean(parsed.analytics),
      marketing: Boolean(parsed.marketing),
      updatedAt: typeof parsed.updatedAt === 'string' ? parsed.updatedAt : '',
    };
  } catch {
    return null;
  }
}

export function writeConsent(preferences: Pick<ConsentPreferences, 'analytics' | 'marketing'>): ConsentPreferences {
  const next: ConsentPreferences = {
    analytics: preferences.analytics,
    marketing: preferences.marketing,
    updatedAt: new Date().toISOString(),
  };

  if (typeof window !== 'undefined') {
    try {
      window.localStorage.setItem(CONSENT_STORAGE_KEY, JSON.stringify(next));
    } catch {
      // Ignore storage failures.
    }
  }

  return next;
}
