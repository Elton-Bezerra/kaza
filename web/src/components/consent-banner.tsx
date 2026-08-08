"use client";

import { useEffect, useMemo, useState } from 'react';
import type { ConsentPreferences } from '@/lib/consent';

type ConsentBannerProps = {
  preferences: ConsentPreferences;
  onSave: (preferences: Pick<ConsentPreferences, 'analytics' | 'marketing'>) => void;
  onClose: () => void;
  visible: boolean;
};

export function ConsentBanner({ preferences, onSave, onClose, visible }: ConsentBannerProps) {
  const [analytics, setAnalytics] = useState(preferences.analytics);
  const [marketing, setMarketing] = useState(preferences.marketing);

  useEffect(() => {
    setAnalytics(preferences.analytics);
    setMarketing(preferences.marketing);
  }, [preferences.analytics, preferences.marketing]);

  const summary = useMemo(() => {
    if (analytics && marketing) {
      return 'Melhoria do site e novidades.';
    }

    if (analytics) {
      return 'Melhoria do site.';
    }

    if (marketing) {
      return 'Novidades.';
    }

    return 'Somente o necessário.';
  }, [analytics, marketing]);

  if (!visible) {
    return null;
  }

  return (
    <section className="consent-banner" aria-label="Preferências do site">
      <div className="consent-layout">
        <div>
          <p className="eyebrow" style={{ marginBottom: 12 }}>
            Preferências
          </p>
          <h2>Como você quer usar o site?</h2>
          <p>Você pode permitir melhorias de navegação e receber novidades da Kaza. Isso pode ser alterado depois.</p>
          <p className="help-text">Status atual: {summary}</p>
        </div>
        <div>
          <div className="consent-options">
            <label className="checkbox">
              <input type="checkbox" checked={analytics} onChange={(event) => setAnalytics(event.target.checked)} />
              <span>Permitir que a Kaza entenda como o site está sendo usado.</span>
            </label>
            <label className="checkbox">
              <input type="checkbox" checked={marketing} onChange={(event) => setMarketing(event.target.checked)} />
              <span>Receber novidades e conteúdos da Kaza.</span>
            </label>
          </div>
          <div className="consent-actions">
            <button className="button button-primary" type="button" onClick={() => onSave({ analytics, marketing })}>
              Salvar escolhas
            </button>
            <button
              className="button button-ghost"
              type="button"
              onClick={() => {
                setAnalytics(false);
                setMarketing(false);
                onSave({ analytics: false, marketing: false });
                onClose();
              }}
            >
              Só o necessário
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
