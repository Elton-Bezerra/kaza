export const SITE_NAME = 'Kaza';
export const SITE_DESCRIPTION =
  'Kaza ajuda síndicos e administradoras de condomínios pequenos a organizar cobrança, despesas e prestação de contas.';

const FALLBACK_SITE_URL = 'http://localhost:3000';
const FALLBACK_API_BASE_URL = 'http://localhost:8080';

export const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL ?? FALLBACK_SITE_URL).replace(/\/$/, '');
export const API_BASE_URL = (process.env.NEXT_PUBLIC_KAZA_API_BASE_URL ?? FALLBACK_API_BASE_URL).replace(/\/$/, '');
export const LEAD_PATH = process.env.NEXT_PUBLIC_KAZA_LEAD_PATH ?? '/api/v1/onboarding/leads';
export const ANALYTICS_PATH = process.env.NEXT_PUBLIC_KAZA_ANALYTICS_PATH ?? '/api/v1/public/landing-events';
export const UNIT_PRICE_PER_MONTH = Number(process.env.NEXT_PUBLIC_KAZA_UNIT_PRICE_PER_MONTH ?? '9.99');
export const UNIT_PRICE_LABEL = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
}).format(UNIT_PRICE_PER_MONTH);
export const TEN_UNIT_PRICE_LABEL = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
}).format(UNIT_PRICE_PER_MONTH * 10);

export function resolveApiUrl(path: string): string {
  if (!API_BASE_URL) {
    return path;
  }

  return new URL(path, API_BASE_URL).toString();
}
