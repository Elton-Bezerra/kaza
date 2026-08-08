"use client";

import { type ChangeEvent, type FormEvent, useEffect, useState } from 'react';
import {
  acceptInvitation,
  deleteCurrentDocument,
  fetchCurrentApplication,
  fetchInvitation,
  listCurrentDocuments,
  submitCurrentApplication,
  type ApplicationInvitationResponse,
  type PublicApplicationResponse,
  type PublicDocumentResponse,
  updateCurrentApplication,
  uploadCurrentDocument,
} from '@/lib/public-onboarding-api';

type UnitDraft = {
  identifier: string;
  idealFraction: string;
};

type ApplicationDraft = {
  responsibleName: string;
  responsibleEmail: string;
  responsiblePhone: string;
  taxId: string;
  condominiumName: string;
  addressLine: string;
  addressCity: string;
  addressState: string;
  postalCode: string;
  subscriptionPricePerUnit: string;
  units: UnitDraft[];
};

const EMPTY_UNIT: UnitDraft = {
  identifier: '',
  idealFraction: '',
};

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function parseDecimal(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number.parseFloat(trimmed.replace(',', '.'));
  return Number.isFinite(parsed) ? parsed : null;
}

function fromApplication(application: PublicApplicationResponse): ApplicationDraft {
  return {
    responsibleName: application.responsibleName ?? '',
    responsibleEmail: application.responsibleEmail ?? '',
    responsiblePhone: application.responsiblePhone ?? '',
    taxId: application.taxId ?? '',
    condominiumName: application.condominiumName ?? '',
    addressLine: application.addressLine ?? '',
    addressCity: application.addressCity ?? '',
    addressState: application.addressState ?? '',
    postalCode: application.postalCode ?? '',
    subscriptionPricePerUnit: application.subscriptionPricePerUnit == null ? '' : String(application.subscriptionPricePerUnit),
    units: application.units.length > 0 ? application.units.map((unit) => ({
      identifier: unit.identifier,
      idealFraction: String(unit.idealFraction),
    })) : [{ ...EMPTY_UNIT }],
  };
}

function emptyDraft(): ApplicationDraft {
  return {
    responsibleName: '',
    responsibleEmail: '',
    responsiblePhone: '',
    taxId: '',
    condominiumName: '',
    addressLine: '',
    addressCity: '',
    addressState: '',
    postalCode: '',
    subscriptionPricePerUnit: '',
    units: [{ ...EMPTY_UNIT }],
  };
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

function isCompleteUnit(unit: UnitDraft): boolean {
  return Boolean(unit.identifier.trim() && unit.idealFraction.trim());
}

export function InvitationApplicationFlow({ token }: { token: string }) {
  const [invitation, setInvitation] = useState<ApplicationInvitationResponse | null>(null);
  const [application, setApplication] = useState<PublicApplicationResponse | null>(null);
  const [draft, setDraft] = useState<ApplicationDraft>(emptyDraft);
  const [documents, setDocuments] = useState<PublicDocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionState, setActionState] = useState<'idle' | 'accepting' | 'saving' | 'submitting' | 'uploading'>('idle');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  async function refreshApplication() {
    const current = await fetchCurrentApplication();
    setApplication(current);
    setDraft(fromApplication(current));
    try {
      setDocuments(await listCurrentDocuments());
    } catch {
      setDocuments([]);
    }
  }

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      setLoading(true);
      setErrorMessage(null);

      try {
        const currentInvitation = await fetchInvitation(token);
        if (cancelled) {
          return;
        }
        setInvitation(currentInvitation);

        try {
          await refreshApplication();
          if (cancelled) {
            return;
          }
          setFeedback('Retomamos seu onboarding já aceito.');
        } catch {
          setApplication(null);
          setDocuments([]);
          setDraft(emptyDraft());
          setFeedback(null);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }
        setErrorMessage(error instanceof Error ? error.message : 'Não foi possível carregar a página.');
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, [token]);

  async function handleAccept() {
    setActionState('accepting');
    setFeedback(null);
    setErrorMessage(null);

    try {
      const accepted = await acceptInvitation(token);
      setApplication(accepted);
      setDraft(fromApplication(accepted));
      try {
        setDocuments(await listCurrentDocuments());
      } catch {
        setDocuments([]);
      }
      setInvitation((current) =>
        current
          ? {
              ...current,
              status: 'ACCEPTED',
              acceptedAt: new Date().toISOString(),
              applicationId: accepted.id,
            }
          : current,
      );
      setFeedback('Convite aceito. Você já pode continuar o onboarding.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Não foi possível aceitar o convite.');
    } finally {
      setActionState('idle');
    }
  }

  async function handleSaveDraft(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setActionState('saving');
    setFeedback(null);
    setErrorMessage(null);

    try {
      const payload = buildPayload();
      const updated = await updateCurrentApplication(payload);
      setApplication(updated);
      setDraft(fromApplication(updated));
      setFeedback('Rascunho salvo com sucesso.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Não foi possível salvar o rascunho.');
    } finally {
      setActionState('idle');
    }
  }

  async function handleSubmit() {
    setActionState('submitting');
    setFeedback(null);
    setErrorMessage(null);

    try {
      const payload = buildPayload();
      const updatedDraft = await updateCurrentApplication(payload);
      setApplication(updatedDraft);
      setDraft(fromApplication(updatedDraft));
      const updated = await submitCurrentApplication();
      setApplication(updated);
      setDraft(fromApplication(updated));
      setFeedback('Aplicação enviada para análise.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Não foi possível enviar a aplicação.');
    } finally {
      setActionState('idle');
    }
  }

  async function handleUpload() {
    if (!selectedFile) {
      setErrorMessage('Escolha um arquivo para anexar.');
      return;
    }

    setActionState('uploading');
    setFeedback(null);
    setErrorMessage(null);

    try {
      await uploadCurrentDocument(selectedFile);
      setDocuments(await listCurrentDocuments());
      setSelectedFile(null);
      setFeedback('Documento anexado.');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Não foi possível anexar o documento.');
    } finally {
      setActionState('idle');
    }
  }

  async function handleDeleteDocument(documentId: string) {
    try {
      await deleteCurrentDocument(documentId);
      setDocuments(await listCurrentDocuments());
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'Não foi possível excluir o documento.');
    }
  }

  function updateUnit(index: number, field: keyof UnitDraft, value: string) {
    setDraft((current) => ({
      ...current,
      units: current.units.map((unit, unitIndex) =>
        unitIndex === index ? { ...unit, [field]: value } : unit,
      ),
    }));
  }

  function buildPayload() {
    const payloadUnits = draft.units.every((unit) => {
      const idealFraction = parseDecimal(unit.idealFraction);
      return isCompleteUnit(unit) && idealFraction !== null;
    })
      ? draft.units.map((unit) => ({
          identifier: unit.identifier.trim(),
          idealFraction: parseDecimal(unit.idealFraction)!,
        }))
      : null;

    return {
      responsibleName: blankToNull(draft.responsibleName),
      responsibleEmail: blankToNull(draft.responsibleEmail),
      responsiblePhone: blankToNull(draft.responsiblePhone),
      taxId: (() => {
        const digits = blankToNull(draft.taxId)?.replace(/\D/g, '') ?? null;
        return digits && digits.length > 0 ? digits : null;
      })(),
      condominiumName: blankToNull(draft.condominiumName),
      addressLine: blankToNull(draft.addressLine),
      addressCity: blankToNull(draft.addressCity),
      addressState: blankToNull(draft.addressState)?.toUpperCase(),
      postalCode: (() => {
        const digits = blankToNull(draft.postalCode)?.replace(/\D/g, '') ?? null;
        return digits && digits.length > 0 ? digits : null;
      })(),
      subscriptionPricePerUnit: parseDecimal(draft.subscriptionPricePerUnit),
      proposedUnitCount: payloadUnits ? payloadUnits.length : null,
      units: payloadUnits?.map((unit) => ({
        identifier: unit.identifier,
        idealFraction: unit.idealFraction,
      })) ?? null,
    };
  }

  function addUnit() {
    setDraft((current) => ({
      ...current,
      units: [...current.units, { ...EMPTY_UNIT }],
    }));
  }

  function removeUnit(index: number) {
    setDraft((current) => ({
      ...current,
      units: current.units.length === 1 ? [{ ...EMPTY_UNIT }] : current.units.filter((_, unitIndex) => unitIndex !== index),
    }));
  }

  const invitationExpired = invitation?.status === 'EXPIRED';
  const invitationAccepted = invitation?.status === 'ACCEPTED';

  if (loading) {
    return (
      <main className="page public-invitation-page">
        <section className="section-card">
          <p className="eyebrow">Convite</p>
          <h1>Carregando sua aplicação...</h1>
          <p>Estamos verificando o convite e preparando o onboarding.</p>
        </section>
      </main>
    );
  }

  if (errorMessage && !invitation) {
    return (
      <main className="page public-invitation-page">
        <section className="section-card">
          <p className="eyebrow">Convite</p>
          <h1>Não foi possível abrir este convite</h1>
          <p>{errorMessage}</p>
        </section>
      </main>
    );
  }

  return (
    <main className="page public-invitation-page">
      <section className="section-card">
        <p className="eyebrow">Convite para onboarding</p>
        <h1>{invitation?.leadName ?? 'Lead'} · Kaza</h1>
        <p className="hero-lead">
          {invitationAccepted
            ? 'Seu convite já foi aceito. Continue preenchendo a aplicação abaixo.'
            : invitationExpired
              ? 'Este convite expirou. Fale com o time Kaza para receber um novo.'
              : `Olá, ${invitation?.leadName ?? 'tudo bem'}? Seu convite foi enviado para ${invitation?.leadEmail ?? ''}.`}
        </p>

        <div className="public-invitation-meta">
          <span className="admin-meta-chip">{invitation?.status ?? '—'}</span>
          <span className="admin-chip">expira em {formatTimestamp(invitation?.expiresAt)}</span>
          <span className="admin-chip">aceito em {formatTimestamp(invitation?.acceptedAt)}</span>
        </div>

        {!invitationAccepted && !invitationExpired ? (
          <div className="public-invitation-actions">
            <button className="button button-primary" type="button" disabled={actionState === 'accepting'} onClick={() => void handleAccept()}>
              {actionState === 'accepting' ? 'Aceitando...' : 'Aceitar convite'}
            </button>
          </div>
        ) : null}

        {feedback ? <div className="alert alert-success">{feedback}</div> : null}
        {errorMessage ? <div className="alert alert-error">{errorMessage}</div> : null}
      </section>

      {application ? (
        <div className="public-invitation-grid">
          <section className="lead-card">
            <div className="form-head">
              <p className="eyebrow">Aplicação</p>
              <h2>Dados básicos</h2>
              <p>Salve os dados do responsável, do condomínio e das unidades para concluir o onboarding.</p>
            </div>

            <form className="form-grid public-form-grid" onSubmit={handleSaveDraft}>
              <div className="field">
                <label className="label" htmlFor="responsibleName">
                  Responsável
                </label>
                <input
                  id="responsibleName"
                  className="input"
                  value={draft.responsibleName}
                  onChange={(event) => setDraft((current) => ({ ...current, responsibleName: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="responsibleEmail">
                  E-mail
                </label>
                <input
                  id="responsibleEmail"
                  className="input"
                  type="email"
                  value={draft.responsibleEmail}
                  onChange={(event) => setDraft((current) => ({ ...current, responsibleEmail: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="responsiblePhone">
                  Telefone
                </label>
                <input
                  id="responsiblePhone"
                  className="input"
                  value={draft.responsiblePhone}
                  onChange={(event) => setDraft((current) => ({ ...current, responsiblePhone: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="taxId">
                  CPF/CNPJ
                </label>
                <input
                  id="taxId"
                  className="input"
                  value={draft.taxId}
                  onChange={(event) => setDraft((current) => ({ ...current, taxId: event.target.value }))}
                />
              </div>

              <div className="field field--full">
                <label className="label" htmlFor="condominiumName">
                  Condomínio
                </label>
                <input
                  id="condominiumName"
                  className="input"
                  value={draft.condominiumName}
                  onChange={(event) => setDraft((current) => ({ ...current, condominiumName: event.target.value }))}
                />
              </div>

              <div className="field field--full">
                <label className="label" htmlFor="addressLine">
                  Endereço
                </label>
                <input
                  id="addressLine"
                  className="input"
                  value={draft.addressLine}
                  onChange={(event) => setDraft((current) => ({ ...current, addressLine: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="addressCity">
                  Cidade
                </label>
                <input
                  id="addressCity"
                  className="input"
                  value={draft.addressCity}
                  onChange={(event) => setDraft((current) => ({ ...current, addressCity: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="addressState">
                  Estado
                </label>
                <input
                  id="addressState"
                  className="input"
                  value={draft.addressState}
                  maxLength={2}
                  onChange={(event) => setDraft((current) => ({ ...current, addressState: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="postalCode">
                  CEP
                </label>
                <input
                  id="postalCode"
                  className="input"
                  value={draft.postalCode}
                  onChange={(event) => setDraft((current) => ({ ...current, postalCode: event.target.value }))}
                />
              </div>

              <div className="field">
                <label className="label" htmlFor="subscriptionPricePerUnit">
                  Preço por unidade
                </label>
                <input
                  id="subscriptionPricePerUnit"
                  className="input"
                  inputMode="decimal"
                  value={draft.subscriptionPricePerUnit}
                  onChange={(event) =>
                    setDraft((current) => ({ ...current, subscriptionPricePerUnit: event.target.value }))
                  }
                />
              </div>

              <div className="field field--full">
                <div className="public-section-header">
                  <div>
                    <p className="eyebrow">Unidades</p>
                    <p>Informe os identificadores e frações ideais.</p>
                  </div>
                  <button className="button button-secondary" type="button" onClick={addUnit}>
                    Adicionar unidade
                  </button>
                </div>

                <div className="public-unit-list">
                  {draft.units.map((unit, index) => (
                    <div className="public-unit-row" key={`${index}-${unit.identifier}`}>
                      <input
                        className="input"
                        placeholder="Ex.: 101"
                        value={unit.identifier}
                        onChange={(event) => updateUnit(index, 'identifier', event.target.value)}
                      />
                      <input
                        className="input"
                        placeholder="0.50000000"
                        value={unit.idealFraction}
                        onChange={(event) => updateUnit(index, 'idealFraction', event.target.value)}
                      />
                      <button className="button button-ghost" type="button" onClick={() => removeUnit(index)}>
                        Remover
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              <div className="field field--full">
                <div className="form-actions">
                  <button className="button button-primary" type="submit" disabled={actionState === 'saving'}>
                    {actionState === 'saving' ? 'Salvando...' : 'Salvar rascunho'}
                  </button>
                  <button className="button button-secondary" type="button" disabled={actionState === 'submitting'} onClick={() => void handleSubmit()}>
                    {actionState === 'submitting' ? 'Enviando...' : 'Enviar aplicação'}
                  </button>
                </div>
              </div>
            </form>
          </section>

          <section className="lead-card">
            <div className="form-head">
              <p className="eyebrow">Documentos</p>
              <h2>Comprovações</h2>
              <p>Envie ao menos um documento de suporte em PDF, JPEG ou PNG.</p>
            </div>

            <div className="field">
              <label className="label" htmlFor="supportingDocument">
                Arquivo
              </label>
              <input
                id="supportingDocument"
                className="input"
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                onChange={(event: ChangeEvent<HTMLInputElement>) => setSelectedFile(event.target.files?.[0] ?? null)}
              />
            </div>

            <div className="form-actions">
              <button className="button button-primary" type="button" disabled={actionState === 'uploading'} onClick={() => void handleUpload()}>
                {actionState === 'uploading' ? 'Anexando...' : 'Anexar documento'}
              </button>
            </div>

            <div className="public-document-list">
              {documents.length === 0 ? (
                <p className="help-text">Nenhum documento anexado ainda.</p>
              ) : (
                documents.map((document) => (
                  <article className="public-document-item" key={document.id}>
                    <div>
                      <strong>{document.filename}</strong>
                      <p>
                        {document.scanStatus} · {Math.round(document.sizeBytes / 1024)} KB · {formatTimestamp(document.uploadedAt)}
                      </p>
                    </div>
                    <button className="button button-ghost" type="button" onClick={() => void handleDeleteDocument(document.id)}>
                      Excluir
                    </button>
                  </article>
                ))
              )}
            </div>
          </section>
        </div>
      ) : null}
    </main>
  );
}
