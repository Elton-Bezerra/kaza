import { ANALYTICS_PATH, resolveApiUrl } from '@/lib/site';
import type { Attribution } from '@/lib/attribution';

export type FunnelEventName =
  | 'page_view'
  | 'cta_view'
  | 'cta_click'
  | 'form_start'
  | 'field_complete'
  | 'submit_success'
  | 'submit_error';

export type FunnelEvent = {
  name: FunnelEventName;
  occurredAt: string;
  pagePath: string;
  pageTitle: string;
  attribution: Attribution;
  location?: string;
  field?: string;
  statusCode?: number;
};

export function buildFunnelEvent(
  name: FunnelEventName,
  payload: Omit<FunnelEvent, 'name' | 'occurredAt'>,
): FunnelEvent {
  return {
    name,
    occurredAt: new Date().toISOString(),
    ...payload,
  };
}

export function trackFunnelEvent(event: FunnelEvent, consentGranted: boolean): void {
  if (!consentGranted || typeof window === 'undefined') {
    return;
  }

  const targetUrl = resolveApiUrl(ANALYTICS_PATH);
  const body = JSON.stringify(event);

  if (navigator.sendBeacon) {
    const blob = new Blob([body], { type: 'application/json' });
    navigator.sendBeacon(targetUrl, blob);
    return;
  }

  fetch(targetUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body,
    keepalive: true,
  }).catch(() => undefined);
}
