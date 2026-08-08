"use client";

import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { performAdminLogout, startAdminAuthFlow } from '@/lib/admin-auth';
import {
  AdminApiError,
  fetchAdminApplications,
  fetchAdminLeads,
  fetchAdminSession,
  fetchAdminSummary,
  inviteAdminLead,
  type AdminApplicationRecord,
  type AdminLeadRecord,
  type AdminSummary,
} from '@/lib/admin-api';

type Tab = 'leads' | 'applications';
type SessionState = 'checking' | 'ready' | 'unauthenticated' | 'forbidden' | 'error';

type AsyncState<T> = {
  status: 'idle' | 'loading' | 'ready' | 'error';
  data: T | null;
  error: string | null;
};

type Filters = {
  search: string;
  status: string;
  source: string;
};

const INITIAL_FILTERS: Filters = {
  search: '',
  status: 'all',
  source: 'all',
};

const SUPER_ADMIN_ROLE = 'SUPER_ADMIN';
const GENERIC_LOAD_ERROR = 'Não foi possível carregar os dados administrativos.';
const GENERIC_AUTH_ERROR = 'Não foi possível validar sua sessão.';
const GENERIC_DENIED_MESSAGE = 'Sua sessão não tem permissão para esta área.';

function createAsyncState<T>(status: AsyncState<T>['status'], data: T | null, error: string | null): AsyncState<T> {
  return { status, data, error };
}

function formatTimestamp(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function normalizeText(value: string | null | undefined): string {
  return (value ?? '').toLowerCase();
}

function filterRecord(record: AdminLeadRecord | AdminApplicationRecord, filters: Filters): boolean {
  const search = filters.search.trim().toLowerCase();
  if (search) {
    const haystack = [
      'name' in record ? record.name : record.applicantName,
      'email' in record ? record.email : null,
      'applicantEmail' in record ? record.applicantEmail : null,
      'phone' in record ? record.phone : null,
      'applicantPhone' in record ? record.applicantPhone : null,
      'condominiumName' in record ? record.condominiumName : null,
      record.status,
      record.source,
      record.attribution?.utmSource,
      record.attribution?.utmCampaign,
      record.attribution?.landingPath,
      record.id,
    ]
      .filter(Boolean)
      .map((item) => normalizeText(item as string))
      .join(' ');

    if (!haystack.includes(search)) {
      return false;
    }
  }

  if (filters.status !== 'all' && normalizeText(record.status) !== normalizeText(filters.status)) {
    return false;
  }

  if (filters.source !== 'all' && normalizeText(record.source) !== normalizeText(filters.source)) {
    return false;
  }

  return true;
}

function uniqueValues(values: Array<string | null | undefined>): string[] {
  return [...new Set(values.filter((value): value is string => Boolean(value)))].sort((a, b) => a.localeCompare(b));
}

function formatLabel(value: string): string {
  return value
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

function StatusPill({ value }: { value: string }) {
  return <span className="admin-pill">{formatLabel(value)}</span>;
}

function SectionState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="admin-state">
      <strong>{title}</strong>
      <p>{description}</p>
      {action ? <div className="admin-state-actions">{action}</div> : null}
    </div>
  );
}

function SummaryCard({
  label,
  value,
  note,
}: {
  label: string;
  value: number | string;
  note: string;
}) {
  return (
    <article className="admin-summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
      <p>{note}</p>
    </article>
  );
}

function LoadingRows() {
  return (
    <div className="admin-list admin-list--loading" aria-hidden="true">
      {Array.from({ length: 5 }).map((_, index) => (
        <div className="admin-list-row admin-list-row--skeleton" key={index}>
          <div className="admin-skeleton admin-skeleton--title" />
          <div className="admin-skeleton admin-skeleton--line" />
          <div className="admin-skeleton admin-skeleton--line admin-skeleton--short" />
        </div>
      ))}
    </div>
  );
}

function SummarySkeleton() {
  return (
    <>
      {Array.from({ length: 4 }).map((_, index) => (
        <div className="admin-summary-card admin-summary-card--skeleton" key={index}>
          <div className="admin-skeleton admin-skeleton--summary" />
          <div className="admin-skeleton admin-skeleton--summary-line" />
          <div className="admin-skeleton admin-skeleton--summary-line admin-skeleton--short" />
        </div>
      ))}
    </>
  );
}

export function AdminDashboard() {
  const [sessionState, setSessionState] = useState<SessionState>('checking');
  const [summaryState, setSummaryState] = useState<AsyncState<AdminSummary>>(createAsyncState<AdminSummary>('idle', null, null));
  const [leadsState, setLeadsState] = useState<AsyncState<{ items: AdminLeadRecord[] }>>(
    createAsyncState<{ items: AdminLeadRecord[] }>('idle', null, null),
  );
  const [applicationsState, setApplicationsState] = useState<AsyncState<{ items: AdminApplicationRecord[] }>>(
    createAsyncState<{ items: AdminApplicationRecord[] }>('idle', null, null),
  );
  const [activeTab, setActiveTab] = useState<Tab>('leads');
  const [leadFilters, setLeadFilters] = useState<Filters>(INITIAL_FILTERS);
  const [applicationFilters, setApplicationFilters] = useState<Filters>(INITIAL_FILTERS);
  const [selectedLeadId, setSelectedLeadId] = useState<string | null>(null);
  const [selectedApplicationId, setSelectedApplicationId] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [leadInvitationState, setLeadInvitationState] = useState<'idle' | 'sending' | 'success' | 'error'>('idle');
  const [leadInvitationMessage, setLeadInvitationMessage] = useState<string | null>(null);
  const autoLoginStarted = useRef(false);

  const resetDashboardData = useCallback(() => {
    setSummaryState(createAsyncState<AdminSummary>('idle', null, null));
    setLeadsState(createAsyncState<{ items: AdminLeadRecord[] }>('idle', null, null));
    setApplicationsState(createAsyncState<{ items: AdminApplicationRecord[] }>('idle', null, null));
    setSelectedLeadId(null);
    setSelectedApplicationId(null);
  }, []);

  const markUnauthenticated = useCallback(() => {
    resetDashboardData();
    setSessionState('unauthenticated');
  }, [resetDashboardData]);

  const markForbidden = useCallback(() => {
    resetDashboardData();
    setSessionState('forbidden');
  }, [resetDashboardData]);

  const markError = useCallback(() => {
    resetDashboardData();
    setSessionState('error');
  }, [resetDashboardData]);

  const signIn = useCallback(async () => {
    try {
      await startAdminAuthFlow();
    } catch {
      markError();
    }
  }, [markError]);

  const signOut = useCallback(async () => {
    await performAdminLogout();
  }, []);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();

    resetDashboardData();
    setSessionState('checking');

    async function bootstrapSession() {
      try {
        const session = await fetchAdminSession(controller.signal);

        if (cancelled) {
          return;
        }

        if (session?.authenticated === false) {
          markUnauthenticated();
          if (!autoLoginStarted.current) {
            autoLoginStarted.current = true;
            void signIn();
          }
          return;
        }

        if (session?.roles?.length && !session.roles.includes(SUPER_ADMIN_ROLE)) {
          markForbidden();
          return;
        }

        setSessionState('ready');
      } catch (error) {
        if (cancelled) {
          return;
        }

        if (error instanceof AdminApiError) {
          if (error.status === 401) {
            markUnauthenticated();
            if (!autoLoginStarted.current) {
              autoLoginStarted.current = true;
              void signIn();
            }
            return;
          }

          if (error.status === 403) {
            markForbidden();
            return;
          }
        }

        markError();
      }
    }

    void bootstrapSession();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [markError, markForbidden, markUnauthenticated, reloadKey, resetDashboardData, signIn]);

  useEffect(() => {
    if (sessionState !== 'ready') {
      return;
    }

    let cancelled = false;
    const controller = new AbortController();

    setSummaryState((current) => (current.data ? current : createAsyncState<AdminSummary>('loading', null, null)));
    setLeadsState((current) => (current.data ? current : createAsyncState<{ items: AdminLeadRecord[] }>('loading', null, null)));
    setApplicationsState((current) =>
      current.data ? current : createAsyncState<{ items: AdminApplicationRecord[] }>('loading', null, null),
    );

    async function loadDashboard() {
      try {
        const [summary, leads, applications] = await Promise.all([
          fetchAdminSummary(controller.signal),
          fetchAdminLeads(undefined, controller.signal),
          fetchAdminApplications(undefined, controller.signal),
        ]);

        if (cancelled) {
          return;
        }

        setSummaryState(
          createAsyncState(
            'ready',
            summary ?? {
              totalLeads: 0,
              newLeads: 0,
              totalApplications: 0,
              pendingApplications: 0,
              leadsByStatus: [],
              applicationsByStatus: [],
              leadSources: [],
              applicationSources: [],
              generatedAt: null,
            },
            null,
          ),
        );
        setLeadsState(createAsyncState('ready', { items: leads?.items ?? [] }, null));
        setApplicationsState(createAsyncState('ready', { items: applications?.items ?? [] }, null));
      } catch (error) {
        if (cancelled) {
          return;
        }

        if (error instanceof AdminApiError) {
          if (error.status === 401) {
            markUnauthenticated();
            return;
          }

          if (error.status === 403) {
            markForbidden();
            return;
          }
        }

        setSummaryState(createAsyncState<AdminSummary>('error', null, GENERIC_LOAD_ERROR));
        setLeadsState(createAsyncState<{ items: AdminLeadRecord[] }>('error', null, GENERIC_LOAD_ERROR));
        setApplicationsState(createAsyncState<{ items: AdminApplicationRecord[] }>('error', null, GENERIC_LOAD_ERROR));
      }
    }

    void loadDashboard();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [markForbidden, markUnauthenticated, sessionState, reloadKey]);

  const leadItems = useMemo(() => {
    const items = leadsState.data?.items ?? [];
    return items.filter((item) => filterRecord(item, leadFilters));
  }, [leadFilters, leadsState.data]);

  const applicationItems = useMemo(() => {
    const items = applicationsState.data?.items ?? [];
    return items.filter((item) => filterRecord(item, applicationFilters));
  }, [applicationFilters, applicationsState.data]);

  useEffect(() => {
    if (leadItems.length === 0) {
      setSelectedLeadId(null);
      return;
    }

    if (selectedLeadId && leadItems.some((item) => item.id === selectedLeadId)) {
      return;
    }

    setSelectedLeadId(leadItems[0]?.id ?? null);
  }, [leadItems, selectedLeadId]);

  useEffect(() => {
    if (applicationItems.length === 0) {
      setSelectedApplicationId(null);
      return;
    }

    if (selectedApplicationId && applicationItems.some((item) => item.id === selectedApplicationId)) {
      return;
    }

    setSelectedApplicationId(applicationItems[0]?.id ?? null);
  }, [applicationItems, selectedApplicationId]);

  useEffect(() => {
    setLeadInvitationState('idle');
    setLeadInvitationMessage(null);
  }, [selectedLeadId]);

  const currentFilters = activeTab === 'leads' ? leadFilters : applicationFilters;
  const currentItems = activeTab === 'leads' ? leadItems : applicationItems;
  const currentSelection =
    activeTab === 'leads'
      ? leadItems.find((item) => item.id === selectedLeadId) ?? null
      : applicationItems.find((item) => item.id === selectedApplicationId) ?? null;
  const statusOptions = useMemo(
    () => uniqueValues((activeTab === 'leads' ? leadsState.data?.items : applicationsState.data?.items)?.map((item) => item.status) ?? []),
    [activeTab, applicationsState.data?.items, leadsState.data?.items],
  );
  const sourceOptions = useMemo(
    () =>
      uniqueValues(
        (activeTab === 'leads' ? leadsState.data?.items : applicationsState.data?.items)?.map((item) => item.source) ?? [],
      ),
    [activeTab, applicationsState.data?.items, leadsState.data?.items],
  );

  function resetFilters(tab: Tab) {
    if (tab === 'leads') {
      setLeadFilters(INITIAL_FILTERS);
      return;
    }

    setApplicationFilters(INITIAL_FILTERS);
  }

  function updateFilters(tab: Tab, next: Partial<Filters>) {
    if (tab === 'leads') {
      setLeadFilters((current) => ({ ...current, ...next }));
      return;
    }

    setApplicationFilters((current) => ({ ...current, ...next }));
  }

  async function handleInviteLead(leadId: string) {
    setLeadInvitationState('sending');
    setLeadInvitationMessage(null);

    try {
      const invitation = await inviteAdminLead(leadId);
      setLeadInvitationState('success');
      setLeadInvitationMessage(`Convite enviado. Expira em ${formatTimestamp(invitation.expiresAt)}.`);
      setReloadKey((value) => value + 1);
    } catch (error) {
      setLeadInvitationState('error');
      setLeadInvitationMessage(error instanceof Error ? error.message : 'Não foi possível enviar o convite.');
    }
  }

  function renderListItem(item: AdminLeadRecord | AdminApplicationRecord, selected: boolean) {
    const isLead = activeTab === 'leads';
    const primaryLabel = isLead ? (item as AdminLeadRecord).name : (item as AdminApplicationRecord).applicantName;
    const secondaryLabel = isLead
      ? (item as AdminLeadRecord).email ?? (item as AdminLeadRecord).phone ?? 'Sem contato'
      : (item as AdminApplicationRecord).applicantEmail ?? (item as AdminApplicationRecord).applicantPhone ?? 'Sem contato';
    const timestamp = isLead
      ? (item as AdminLeadRecord).createdAt
      : (item as AdminApplicationRecord).submittedAt ?? (item as AdminApplicationRecord).createdAt;

    return (
      <button
        key={item.id}
        type="button"
        className={`admin-list-row ${selected ? 'is-selected' : ''}`}
        onClick={() => {
          if (isLead) {
            setSelectedLeadId(item.id);
          } else {
            setSelectedApplicationId(item.id);
          }
        }}
      >
        <div className="admin-list-row__header">
          <div>
            <strong>{primaryLabel}</strong>
            <span>{secondaryLabel}</span>
          </div>
          <StatusPill value={item.status} />
        </div>
        <div className="admin-list-row__meta">
          <span>{item.source ?? 'sem source'}</span>
          <span>{formatTimestamp(timestamp)}</span>
        </div>
      </button>
    );
  }

  function renderDetail(record: AdminLeadRecord | AdminApplicationRecord | null) {
    if (!record) {
      return (
        <SectionState
          title="Selecione um registro"
          description="Escolha um item na lista para visualizar contatos, status, origem e UTMs."
        />
      );
    }

    const isLead = activeTab === 'leads';

    return (
      <div className="admin-detail">
        <div className="admin-detail__header">
          <div>
            <p className="admin-kicker">{activeTab === 'leads' ? 'Lead' : 'Application'}</p>
            <h3>{isLead ? (record as AdminLeadRecord).name : (record as AdminApplicationRecord).applicantName}</h3>
          </div>
          <StatusPill value={record.status} />
        </div>

        <dl className="admin-detail__grid">
          <div>
            <dt>ID</dt>
            <dd>{record.id}</dd>
          </div>
          <div>
            <dt>Source</dt>
            <dd>{record.source ?? '—'}</dd>
          </div>
          <div>
            <dt>Criado em</dt>
            <dd>{formatTimestamp(record.createdAt)}</dd>
          </div>
          <div>
            <dt>Atualizado em</dt>
            <dd>{formatTimestamp(record.updatedAt)}</dd>
          </div>
          <div>
            <dt>Contato</dt>
            <dd>
              {isLead
                ? (record as AdminLeadRecord).email ?? (record as AdminLeadRecord).phone ?? '—'
                : (record as AdminApplicationRecord).applicantEmail ?? (record as AdminApplicationRecord).applicantPhone ?? '—'}
            </dd>
          </div>
          <div>
            <dt>Contato secundário</dt>
            <dd>
              {isLead
                ? (record as AdminLeadRecord).phone ?? (record as AdminLeadRecord).email ?? '—'
                : (record as AdminApplicationRecord).applicantPhone ?? (record as AdminApplicationRecord).applicantEmail ?? '—'}
            </dd>
          </div>
          {isLead ? (
            <div>
              <dt>Responsável</dt>
              <dd>{(record as AdminLeadRecord).assignedTo ?? '—'}</dd>
            </div>
          ) : (
            <div>
              <dt>Condomínio</dt>
              <dd>{(record as AdminApplicationRecord).condominiumName ?? '—'}</dd>
            </div>
          )}
          {isLead ? (
            <div>
              <dt>Contato feito em</dt>
              <dd>{formatTimestamp((record as AdminLeadRecord).contactedAt)}</dd>
            </div>
          ) : (
            <div>
              <dt>Enviado em</dt>
              <dd>{formatTimestamp((record as AdminApplicationRecord).submittedAt)}</dd>
            </div>
          )}
          {!isLead ? (
            <div>
              <dt>Decisão em</dt>
              <dd>{formatTimestamp((record as AdminApplicationRecord).decisionAt)}</dd>
            </div>
          ) : null}
          {!isLead ? (
            <div>
              <dt>Stage</dt>
              <dd>{(record as AdminApplicationRecord).stage ?? '—'}</dd>
            </div>
          ) : null}
          {!isLead ? (
            <div>
              <dt>Unidades</dt>
              <dd>{(record as AdminApplicationRecord).unitCount ?? '—'}</dd>
            </div>
          ) : null}
        </dl>

        {record.notes ? (
          <div className="admin-detail__notes">
            <dt>Notas</dt>
            <dd>{record.notes}</dd>
          </div>
        ) : null}

        {isLead ? (
          <section className="admin-detail__section" aria-labelledby="admin-detail-invitation">
            <div className="admin-detail__section-header">
              <h4 id="admin-detail-invitation">Convite de aplicação</h4>
              <button
                className="button button-primary"
                type="button"
                disabled={leadInvitationState === 'sending'}
                onClick={() => void handleInviteLead(record.id)}
              >
                {leadInvitationState === 'sending' ? 'Enviando...' : 'Enviar convite'}
              </button>
            </div>
            <p className="admin-detail__section-copy">
              Envie um link seguro, de uso único e com expiração para iniciar o onboarding público deste lead.
            </p>
            {leadInvitationMessage ? (
              <div
                className={`alert ${leadInvitationState === 'error' ? 'alert-error' : 'alert-success'}`}
                role="status"
                aria-live="polite"
              >
                {leadInvitationMessage}
              </div>
            ) : null}
          </section>
        ) : null}

        <section className="admin-detail__section" aria-labelledby="admin-detail-source">
          <h4 id="admin-detail-source">Origem e UTMs</h4>
          <div className="admin-chip-grid">
            <span className="admin-chip">landing: {record.attribution?.landingPath ?? '—'}</span>
            <span className="admin-chip">referrer: {record.attribution?.referrer ?? '—'}</span>
            <span className="admin-chip">utm_source: {record.attribution?.utmSource ?? '—'}</span>
            <span className="admin-chip">utm_medium: {record.attribution?.utmMedium ?? '—'}</span>
            <span className="admin-chip">utm_campaign: {record.attribution?.utmCampaign ?? '—'}</span>
            <span className="admin-chip">utm_content: {record.attribution?.utmContent ?? '—'}</span>
            <span className="admin-chip">utm_term: {record.attribution?.utmTerm ?? '—'}</span>
          </div>
        </section>
      </div>
    );
  }

  if (sessionState === 'checking') {
    return (
      <main className="admin-page" aria-labelledby="admin-dashboard-title">
        <header className="admin-hero">
          <div>
            <p className="eyebrow">Área restrita</p>
            <h1 id="admin-dashboard-title">Painel administrativo</h1>
            <p className="admin-intro">Confirmando sua sessão BFF antes de carregar o painel.</p>
          </div>
        </header>
        <section className="admin-summary" aria-label="Autenticação administrativa">
          <SectionState title="Validando sessão" description="Aguardando confirmação da sessão no servidor." />
        </section>
      </main>
    );
  }

  if (sessionState === 'unauthenticated') {
    return (
      <main className="admin-page" aria-labelledby="admin-dashboard-title">
        <header className="admin-hero">
          <div>
            <p className="eyebrow">Área restrita</p>
            <h1 id="admin-dashboard-title">Painel administrativo</h1>
            <p className="admin-intro">O acesso depende de uma sessão administrativa ativa.</p>
          </div>
        </header>
        <section className="admin-summary" aria-label="Acesso administrativo">
          <SectionState
            title="Sessão ausente"
            description="Entre para iniciar a autenticação híbrida."
            action={
            <button className="button button-primary" type="button" onClick={() => void signIn()}>
                Entrar
            </button>
            }
          />
        </section>
      </main>
    );
  }

  if (sessionState === 'forbidden') {
    return (
      <main className="admin-page" aria-labelledby="admin-dashboard-title">
        <header className="admin-hero">
          <div>
            <p className="eyebrow">Área restrita</p>
            <h1 id="admin-dashboard-title">Painel administrativo</h1>
            <p className="admin-intro">A sessão existe, mas não tem permissão para esta área.</p>
          </div>
        </header>
        <section className="admin-summary" aria-label="Acesso administrativo">
          <SectionState
            title="Acesso indisponível"
            description={GENERIC_DENIED_MESSAGE}
            action={
            <button className="button button-secondary" type="button" onClick={() => void signOut()}>
                Sair
            </button>
            }
          />
        </section>
      </main>
    );
  }

  if (sessionState === 'error') {
    return (
      <main className="admin-page" aria-labelledby="admin-dashboard-title">
        <header className="admin-hero">
          <div>
            <p className="eyebrow">Área restrita</p>
            <h1 id="admin-dashboard-title">Painel administrativo</h1>
            <p className="admin-intro">Não foi possível confirmar a sessão neste momento.</p>
          </div>
        </header>
        <section className="admin-summary" aria-label="Acesso administrativo">
          <SectionState
            title="Área administrativa indisponível"
            description={GENERIC_AUTH_ERROR}
            action={
              <button className="button button-secondary" type="button" onClick={() => setReloadKey((value) => value + 1)}>
                Tentar novamente
              </button>
            }
          />
        </section>
      </main>
    );
  }

  const summaryIsLoading = summaryState.data === null && (summaryState.status === 'idle' || summaryState.status === 'loading');
  const leadsIsLoading = leadsState.data === null && (leadsState.status === 'idle' || leadsState.status === 'loading');
  const applicationsIsLoading =
    applicationsState.data === null && (applicationsState.status === 'idle' || applicationsState.status === 'loading');
  const totalVisible = currentItems.length;

  return (
    <main className="admin-page" aria-labelledby="admin-dashboard-title">
      <header className="admin-hero">
        <div>
          <p className="eyebrow">Área restrita</p>
          <h1 id="admin-dashboard-title">Painel administrativo</h1>
          <p className="admin-intro">
            Operação interna para acompanhar captação, status, origem e próximos passos. O navegador só consulta a BFF
            com sessão confirmada.
          </p>
        </div>
        <div className="admin-hero__meta">
          <span className="admin-meta-chip admin-meta-chip--success">Sessão confirmada</span>
          <button
            className="button button-secondary admin-refresh"
            type="button"
            onClick={() => setReloadKey((value) => value + 1)}
          >
            Recarregar
          </button>
          <button className="button button-ghost admin-refresh" type="button" onClick={() => void signOut()}>
            Sair
          </button>
        </div>
      </header>

      <section className="admin-summary" aria-label="Resumo operacional">
        {summaryIsLoading ? (
          <SummarySkeleton />
        ) : summaryState.status === 'error' ? (
          <SectionState
            title="Não foi possível carregar o resumo"
            description={GENERIC_LOAD_ERROR}
            action={
              <button className="button button-secondary" type="button" onClick={() => setReloadKey((value) => value + 1)}>
                Tentar novamente
              </button>
            }
          />
        ) : (
          <>
            <SummaryCard
              label="Leads totais"
              value={summaryState.data?.totalLeads ?? 0}
              note="Volume bruto recebido pelo funil."
            />
            <SummaryCard label="Leads novos" value={summaryState.data?.newLeads ?? 0} note="Leads ainda sem tratamento." />
            <SummaryCard
              label="Applications totais"
              value={summaryState.data?.totalApplications ?? 0}
              note="Pedidos em onboarding e análise."
            />
            <SummaryCard
              label="Pendências"
              value={summaryState.data?.pendingApplications ?? 0}
              note="Applications que pedem ação imediata."
            />
          </>
        )}
      </section>

      <section className="admin-shell" aria-label="Listas administrativas">
        <div className="admin-panel">
          <div className="admin-tabs" role="tablist" aria-label="Lista administrativa">
            <button
              className={`admin-tab ${activeTab === 'leads' ? 'is-active' : ''}`}
              role="tab"
              aria-selected={activeTab === 'leads'}
              type="button"
              onClick={() => setActiveTab('leads')}
            >
              Leads
            </button>
            <button
              className={`admin-tab ${activeTab === 'applications' ? 'is-active' : ''}`}
              role="tab"
              aria-selected={activeTab === 'applications'}
              type="button"
              onClick={() => setActiveTab('applications')}
            >
              Applications
            </button>
          </div>

          <div className="admin-filters">
            <label className="admin-field">
              <span>Buscar</span>
              <input
                className="input"
                type="search"
                value={currentFilters.search}
                placeholder="Nome, e-mail, telefone, UTM..."
                onChange={(event) => updateFilters(activeTab, { search: event.target.value })}
              />
            </label>

            <label className="admin-field">
              <span>Status</span>
              <select
                className="select"
                value={currentFilters.status}
                onChange={(event) => updateFilters(activeTab, { status: event.target.value })}
              >
                <option value="all">Todos</option>
                {statusOptions.map((status) => (
                  <option key={status} value={status}>
                    {formatLabel(status)}
                  </option>
                ))}
              </select>
            </label>

            <label className="admin-field">
              <span>Source</span>
              <select
                className="select"
                value={currentFilters.source}
                onChange={(event) => updateFilters(activeTab, { source: event.target.value })}
              >
                <option value="all">Todas</option>
                {sourceOptions.map((source) => (
                  <option key={source} value={source}>
                    {source}
                  </option>
                ))}
              </select>
            </label>

            <button className="button button-ghost admin-reset" type="button" onClick={() => resetFilters(activeTab)}>
              Limpar filtros
            </button>
          </div>

          <div className="admin-list-header">
            <strong>{activeTab === 'leads' ? 'Leads' : 'Applications'} visíveis</strong>
            <span>{totalVisible} registros</span>
          </div>

          {activeTab === 'leads' && leadsIsLoading ? (
            <LoadingRows />
          ) : activeTab === 'applications' && applicationsIsLoading ? (
            <LoadingRows />
          ) : activeTab === 'leads' && leadsState.status === 'error' ? (
            <SectionState
              title="Erro ao carregar leads"
              description={GENERIC_LOAD_ERROR}
              action={
                <button className="button button-secondary" type="button" onClick={() => setReloadKey((value) => value + 1)}>
                  Recarregar
                </button>
              }
            />
          ) : activeTab === 'applications' && applicationsState.status === 'error' ? (
            <SectionState
              title="Erro ao carregar applications"
              description={GENERIC_LOAD_ERROR}
              action={
                <button className="button button-secondary" type="button" onClick={() => setReloadKey((value) => value + 1)}>
                  Recarregar
                </button>
              }
            />
          ) : currentItems.length === 0 ? (
            <SectionState
              title="Sem resultados para estes filtros"
              description="Ajuste status, source ou busca para localizar outro conjunto de registros."
              action={
                <button className="button button-secondary" type="button" onClick={() => resetFilters(activeTab)}>
                  Mostrar tudo
                </button>
              }
            />
          ) : (
            <div className="admin-grid">
              <div className="admin-list" role="tabpanel">
                {currentItems.map((item) =>
                  renderListItem(item, activeTab === 'leads' ? item.id === selectedLeadId : item.id === selectedApplicationId),
                )}
              </div>
              <aside className="admin-detail-panel" aria-live="polite">
                {renderDetail(currentSelection)}
              </aside>
            </div>
          )}
        </div>
      </section>
    </main>
  );
}
