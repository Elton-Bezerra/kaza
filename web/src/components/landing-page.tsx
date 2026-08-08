"use client";

import { useEffect, useMemo, useRef, useState } from 'react';
import { ConsentBanner } from '@/components/consent-banner';
import { LeadForm } from '@/components/lead-form';
import { buildFunnelEvent, trackFunnelEvent } from '@/lib/analytics';
import type { Attribution } from '@/lib/attribution';
import { collectAttribution } from '@/lib/attribution';
import { DEFAULT_CONSENT, readConsent, type ConsentPreferences, writeConsent } from '@/lib/consent';
import { SITE_NAME, TEN_UNIT_PRICE_LABEL, UNIT_PRICE_LABEL } from '@/lib/site';

const AUDIENCE = [
  {
    title: 'Síndicos de condomínios pequenos',
    body: 'Que querem sair da planilha e organizar a operação com mais tranquilidade.',
  },
  {
    title: 'Administradoras enxutas',
    body: 'Que atendem poucos empreendimentos e precisam de um processo simples.',
  },
  {
    title: 'Condomínios em crescimento',
    body: 'Que precisam padronizar cobrança, despesas e prestação de contas.',
  },
];

const BENEFITS = [
  {
    title: 'Cobrança mais fácil de explicar',
    body: 'Modelo por unidade ajuda o condomínio a entender o custo mensal sem surpresas.',
  },
  {
    title: 'Menos retrabalho no dia a dia',
    body: 'A operação fica mais organizada para acompanhar pagamentos, despesas e documentos.',
  },
  {
    title: 'Atendimento humano no início',
    body: 'Você conversa com a equipe para entender se a Kaza faz sentido para o seu caso.',
  },
];

const PROCESS = [
  {
    title: 'Você envia o contato',
    body: 'Preenche o formulário com seus dados e informa rapidamente o perfil do condomínio.',
  },
  {
    title: 'A Kaza entende o cenário',
    body: 'A equipe conversa para conhecer o porte, a rotina e a necessidade principal.',
  },
  {
    title: 'Você recebe um próximo passo',
    body: 'Se houver aderência, a conversa segue com uma proposta e alinhamento comercial.',
  },
];

const VALUE = [
  {
    title: `${UNIT_PRICE_LABEL} por unidade`,
    body: 'Preço simples para microcondomínios e condomínios de pequeno porte.',
  },
  {
    title: 'Exemplo de 10 unidades',
    body: `Uma operação com 10 unidades fica em torno de ${TEN_UNIT_PRICE_LABEL} por mês.`,
  },
  {
    title: 'Um sistema para o básico bem feito',
    body: 'Cobrança, despesas e prestação de contas no mesmo fluxo.',
  },
];

function buildPageAttribution(): Attribution {
  return collectAttribution();
}

export function LandingPage() {
  const [consent, setConsent] = useState<ConsentPreferences | null>(null);
  const [consentBannerVisible, setConsentBannerVisible] = useState(false);
  const [attribution, setAttribution] = useState<Attribution>({
    landingPath: '/',
    referrer: null,
    utmSource: null,
    utmMedium: null,
    utmCampaign: null,
    utmContent: null,
    utmTerm: null,
  });
  const [pageViewTracked, setPageViewTracked] = useState(false);
  const [ctaViewTracked, setCtaViewTracked] = useState(false);
  const heroActionsRef = useRef<HTMLDivElement | null>(null);
  const pageTitle = useMemo(() => `${SITE_NAME} · gestão para condomínios pequenos`, []);

  useEffect(() => {
    const storedConsent = readConsent();
    if (storedConsent) {
      setConsent(storedConsent);
      setConsentBannerVisible(false);
    } else {
      setConsent({ ...DEFAULT_CONSENT });
      setConsentBannerVisible(true);
    }

    setAttribution(buildPageAttribution());
  }, []);

  useEffect(() => {
    if (!consent?.analytics || pageViewTracked) {
      return;
    }

    trackFunnelEvent(
      buildFunnelEvent('page_view', {
        pagePath: attribution.landingPath,
        pageTitle,
        attribution,
      }),
      true,
    );
    setPageViewTracked(true);
  }, [attribution, consent?.analytics, pageTitle, pageViewTracked]);

  useEffect(() => {
    if (!consent?.analytics || ctaViewTracked || !heroActionsRef.current) {
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry?.isIntersecting) {
          return;
        }

        trackFunnelEvent(
          buildFunnelEvent('cta_view', {
            pagePath: attribution.landingPath,
            pageTitle,
            attribution,
            location: 'hero-actions',
          }),
          true,
        );
        setCtaViewTracked(true);
        observer.disconnect();
      },
      {
        threshold: 0.55,
      },
    );

    observer.observe(heroActionsRef.current);
    return () => observer.disconnect();
  }, [attribution, consent?.analytics, ctaViewTracked, pageTitle]);

  function persistConsent(nextConsent: Pick<ConsentPreferences, 'analytics' | 'marketing'>) {
    const stored = writeConsent(nextConsent);
    setConsent(stored);
    setConsentBannerVisible(false);

    if (stored.analytics && !pageViewTracked) {
      trackFunnelEvent(
        buildFunnelEvent('page_view', {
          pagePath: attribution.landingPath,
          pageTitle,
          attribution,
        }),
        true,
      );
      setPageViewTracked(true);
    }
  }

  function trackClick(location: string) {
    if (!consent?.analytics) {
      return;
    }

    trackFunnelEvent(
      buildFunnelEvent('cta_click', {
        pagePath: attribution.landingPath,
        pageTitle,
        attribution,
        location,
      }),
      true,
    );
  }

  return (
    <>
      <main className="page">
        <header className="topbar">
          <div className="brand" aria-label={SITE_NAME}>
            <span className="brand-mark">K</span>
            <span className="brand-name">{SITE_NAME}</span>
          </div>
          <nav className="topbar-links" aria-label="Navegação secundária">
            <a href="#beneficios">Benefícios</a>
            <a href="#processo">Como funciona</a>
            <a href="#preco">Preço</a>
            <a href="#lead-form">Fale com a Kaza</a>
            <button type="button" onClick={() => setConsentBannerVisible(true)}>
              Preferências
            </button>
          </nav>
        </header>

        <div className="hero">
          <section className="hero-copy" aria-labelledby="hero-title">
            <p className="eyebrow">Gestão para condomínios pequenos</p>
            <h1 id="hero-title">Cobrança e gestão de condomínio sem complicação.</h1>
            <p className="hero-lead">
              A Kaza ajuda síndicos e administradoras de condomínios pequenos a organizar cobrança, despesas e
              prestação de contas em um fluxo simples e fácil de acompanhar.
            </p>

            <div className="hero-points">
              <div className="point">
                <strong>Feito para pequenos condomínios</strong>
                <span>Ideal para quem quer sair da planilha sem adotar algo pesado demais.</span>
              </div>
              <div className="point">
                <strong>Preço por unidade</strong>
                <span>Um modelo claro para explicar custo e aprovar com facilidade.</span>
              </div>
              <div className="point">
                <strong>Atendimento humano</strong>
                <span>Você conversa com a equipe e entende o próximo passo com clareza.</span>
              </div>
            </div>

            <div className="hero-actions" ref={heroActionsRef}>
              <a
                className="button button-primary"
                href="#lead-form"
                onClick={() => {
                  trackClick('hero-primary');
                }}
              >
                Quero falar com a Kaza
              </a>
              <a
                className="button button-secondary"
                href="#processo"
                onClick={() => {
                  trackClick('hero-secondary');
                }}
              >
                Ver como funciona
              </a>
            </div>

            <div className="metrics" aria-label="Resumo da proposta">
              <div className="stat">
                <strong>4 a 20 unidades</strong>
                <span>Faixa ideal para condomínios pequenos.</span>
              </div>
              <div className="stat">
                <strong>{UNIT_PRICE_LABEL}</strong>
                <span>por unidade ao mês, com preço simples de comunicar.</span>
              </div>
              <div className="stat">
                <strong>Cobrança + contas</strong>
                <span>Um só lugar para acompanhar a rotina financeira.</span>
              </div>
            </div>
          </section>

          <aside className="hero-panel">
            <div className="section-card">
              <p className="eyebrow">Para quem é</p>
              <h2>Quem mais se beneficia da Kaza</h2>
              <div className="card-grid" style={{ marginTop: 18 }}>
                {AUDIENCE.map((card) => (
                  <article className="card" key={card.title}>
                    <h3>{card.title}</h3>
                    <p>{card.body}</p>
                  </article>
                ))}
              </div>
            </div>
          </aside>
        </div>

        <section className="section" id="beneficios">
          <div className="section-card">
            <div className="section-header">
              <div>
                <p className="eyebrow">Benefícios</p>
                <h2>Mais clareza para a rotina do condomínio.</h2>
              </div>
              <a className="button button-secondary" href="#lead-form" onClick={() => trackClick('benefits-cta')}>
                Pedir contato
              </a>
            </div>
            <div className="card-grid" style={{ marginTop: 18 }}>
              {BENEFITS.map((item) => (
                <article className="card" key={item.title}>
                  <h3>{item.title}</h3>
                  <p>{item.body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="processo">
          <div className="section-card">
            <div className="section-header">
              <div>
                <p className="eyebrow">Como funciona</p>
                <h2>Um caminho simples do interesse ao próximo passo.</h2>
              </div>
            </div>
            <div className="process-grid" style={{ marginTop: 18 }}>
              {PROCESS.map((item, index) => (
                <article className="process-card" key={item.title}>
                  <span className="step-index">0{index + 1}</span>
                  <h3>{item.title}</h3>
                  <p>{item.body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="section" id="preco">
          <div className="lead-layout">
            <div className="section-card">
              <p className="eyebrow">Preço</p>
              <h2>Valor simples para condomínios pequenos.</h2>
              <p>
                A Kaza trabalha com um preço por unidade que facilita a decisão. O objetivo é manter a conta clara e
                o começo leve para o síndico ou administradora.
              </p>
              <div className="card-grid" style={{ marginTop: 18 }}>
                {VALUE.map((item) => (
                  <article className="card" key={item.title}>
                    <h3>{item.title}</h3>
                    <p>{item.body}</p>
                  </article>
                ))}
              </div>
            </div>

            <LeadForm analyticsConsent={Boolean(consent?.analytics)} attribution={attribution} />
          </div>
        </section>

        <footer className="footer">
          <span>© 2026 Kaza · gestão para condomínios pequenos.</span>
          <button type="button" onClick={() => setConsentBannerVisible(true)}>
            Preferências
          </button>
        </footer>
      </main>

      <ConsentBanner
        preferences={consent ?? DEFAULT_CONSENT}
        visible={consentBannerVisible}
        onSave={persistConsent}
        onClose={() => setConsentBannerVisible(false)}
      />
    </>
  );
}
