"use client";

import { type FormEvent, useMemo, useRef, useState } from 'react';
import { buildFunnelEvent, trackFunnelEvent } from '@/lib/analytics';
import type { Attribution } from '@/lib/attribution';
import { submitLead } from '@/lib/api';

export type LeadFormProps = {
  analyticsConsent: boolean;
  attribution: Attribution;
};

type Role = 'SINDICO' | 'MORADOR' | 'OUTRO';

type FormState = {
  name: string;
  email: string;
  phone: string;
  role: Role;
  contactConsent: boolean;
  marketingConsent: boolean;
};

const INITIAL_FORM: FormState = {
  name: '',
  email: '',
  phone: '',
  role: 'SINDICO',
  contactConsent: false,
  marketingConsent: false,
};

const PHONE_PATTERN = /^[0-9()+\s-]{10,}$/;

export function LeadForm({ analyticsConsent, attribution }: LeadFormProps) {
  const [form, setForm] = useState<FormState>(INITIAL_FORM);
  const [status, setStatus] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [startTracked, setStartTracked] = useState(false);
  const [touchedFields, setTouchedFields] = useState<Partial<Record<keyof FormState, boolean>>>({});
  const completedFields = useRef(new Set<keyof FormState>());
  const formRef = useRef<HTMLFormElement | null>(null);
  const pagePath = typeof window === 'undefined' ? '/' : `${window.location.pathname}${window.location.search}`;
  const pageTitle = typeof document === 'undefined' ? 'Kaza' : document.title;

  const errors = useMemo(() => {
    return {
      name: form.name.trim().length >= 3 ? '' : 'Informe seu nome completo.',
      email: /\S+@\S+\.\S+/.test(form.email) ? '' : 'Informe um e-mail válido.',
      phone: PHONE_PATTERN.test(form.phone) ? '' : 'Informe um telefone com DDD.',
      role: form.role ? '' : 'Escolha o papel da pessoa que está fazendo o pedido.',
      contactConsent: form.contactConsent ? '' : 'Precisamos dessa autorização para responder.',
    };
  }, [form.contactConsent, form.email, form.name, form.phone, form.role]);

  function markTouched(field: keyof FormState) {
    setTouchedFields((current) => ({
      ...current,
      [field]: true,
    }));
  }

  function shouldShowHelp(field: keyof FormState) {
    return Boolean(touchedFields[field] || status === 'error');
  }

  function emitEvent(
    name: 'form_start' | 'field_complete' | 'submit_success' | 'submit_error',
    extra: Partial<Pick<Parameters<typeof buildFunnelEvent>[1], 'field' | 'location' | 'statusCode'>> = {},
  ) {
    trackFunnelEvent(
      buildFunnelEvent(name, {
        pagePath,
        pageTitle,
        attribution,
        ...extra,
      }),
      analyticsConsent,
    );
  }

  function handleStart() {
    if (!startTracked) {
      setStartTracked(true);
      emitEvent('form_start', { location: 'lead-form' });
    }
  }

  function trackCompletion(field: keyof FormState, completed?: boolean) {
    if (completedFields.current.has(field)) {
      return;
    }

    const isCompleted =
      typeof completed === 'boolean'
        ? completed
        : field === 'name'
          ? form.name.trim().length >= 3
          : field === 'email'
            ? /\S+@\S+\.\S+/.test(form.email)
            : field === 'phone'
              ? PHONE_PATTERN.test(form.phone)
              : field === 'role'
                ? Boolean(form.role)
                : field === 'contactConsent'
                  ? form.contactConsent
                  : form.marketingConsent;

    if (!isCompleted) {
      return;
    }

    completedFields.current.add(field);
    emitEvent('field_complete', { field });
  }

  function trackAllCompletions() {
    (['name', 'email', 'phone', 'role', 'contactConsent', 'marketingConsent'] as const).forEach((field) => {
      trackCompletion(field);
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    handleStart();

    if (!formRef.current?.checkValidity()) {
      formRef.current?.reportValidity();
      setStatus('error');
      setFeedback('Revise os campos obrigatórios antes de enviar.');
      emitEvent('submit_error', { statusCode: 400 });
      return;
    }

    setStatus('submitting');
    setFeedback(null);
    trackAllCompletions();

    try {
      const response = await submitLead({
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        role: form.role,
        contactConsent: form.contactConsent,
        marketingConsent: form.marketingConsent,
        analyticsConsent,
        attribution,
        source: 'web-landing',
      });

      if (!response.ok) {
        let message = 'Não foi possível enviar agora. Tente novamente em instantes.';
        try {
          const payload = (await response.json()) as { message?: string };
          if (payload?.message) {
            message = payload.message;
          }
        } catch {
          // ignore parse errors
        }

        setStatus('error');
        setFeedback(message);
        emitEvent('submit_error', { statusCode: response.status });
        return;
      }

      setStatus('success');
      setFeedback('Recebemos seu interesse. Em breve a equipe Kaza entra em contato.');
      emitEvent('submit_success', { statusCode: response.status });
    } catch {
      setStatus('error');
      setFeedback('Falha de rede ao enviar o formulário. Tente novamente.');
      emitEvent('submit_error', { statusCode: 0 });
    }
  }

  function updateField<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [field]: value }));

    if (field !== 'marketingConsent' && field !== 'contactConsent') {
      return;
    }

    if (field === 'contactConsent' && value) {
      trackCompletion(field, Boolean(value));
    }

    if (field === 'marketingConsent' && value) {
      trackCompletion(field, Boolean(value));
    }
  }

  return (
    <section className="lead-card" id="lead-form" aria-labelledby="lead-form-title">
      <div className="form-head">
        <p className="eyebrow">Contato</p>
        <h2 id="lead-form-title">Fale com a Kaza</h2>
        <p>Deixe seus dados e conte rapidamente sobre o condomínio. A equipe retorna para entender o melhor caminho.</p>
      </div>

      <form ref={formRef} onSubmit={handleSubmit} onFocusCapture={handleStart} className="form-grid" noValidate>
        <div className="field">
          <label className="label" htmlFor="lead-name">
            Nome
          </label>
          <input
            id="lead-name"
            name="name"
            className="input"
            type="text"
            autoComplete="name"
            placeholder="Seu nome"
            value={form.name}
            onChange={(event) => updateField('name', event.target.value)}
            onBlur={() => {
              markTouched('name');
              trackCompletion('name');
            }}
            minLength={3}
            required
          />
          <p className="help-text">{shouldShowHelp('name') ? errors.name : '\u00A0'}</p>
        </div>

        <div className="field">
          <label className="label" htmlFor="lead-email">
            E-mail
          </label>
          <input
            id="lead-email"
            name="email"
            className="input"
            type="email"
            autoComplete="email"
            placeholder="voce@exemplo.com"
            value={form.email}
            onChange={(event) => updateField('email', event.target.value)}
            onBlur={() => {
              markTouched('email');
              trackCompletion('email');
            }}
            required
          />
          <p className="help-text">{shouldShowHelp('email') ? errors.email : '\u00A0'}</p>
        </div>

        <div className="field">
          <label className="label" htmlFor="lead-phone">
            Telefone
          </label>
          <input
            id="lead-phone"
            name="phone"
            className="input"
            type="tel"
            autoComplete="tel"
            inputMode="tel"
            placeholder="(11) 99999-9999"
            value={form.phone}
            onChange={(event) => updateField('phone', event.target.value)}
            onBlur={() => {
              markTouched('phone');
              trackCompletion('phone');
            }}
            pattern="[0-9()+\s-]{10,}"
            required
          />
          <p className="help-text">{shouldShowHelp('phone') ? errors.phone : '\u00A0'}</p>
        </div>

        <div className="field">
          <label className="label" htmlFor="lead-role">
            Você é
          </label>
          <select
            id="lead-role"
            name="role"
            className="select"
            value={form.role}
            onChange={(event) => updateField('role', event.target.value as Role)}
            onBlur={() => {
              markTouched('role');
              trackCompletion('role');
            }}
            required
          >
            <option value="SINDICO">Síndico</option>
            <option value="MORADOR">Morador</option>
            <option value="OUTRO">Outro</option>
          </select>
          <p className="help-text">{errors.role}</p>
        </div>

        <div className="field field--full">
          <div className="checkbox-group">
            <label className="checkbox">
              <input
                name="contactConsent"
                type="checkbox"
                checked={form.contactConsent}
                onChange={(event) => {
                  markTouched('contactConsent');
                  updateField('contactConsent', event.target.checked);
                  if (event.target.checked) {
                    trackCompletion('contactConsent');
                  }
                }}
                required
              />
              <span>
                Autorizo a Kaza a usar meus dados para responder este pedido.
              </span>
            </label>
            <label className="checkbox">
              <input
                name="marketingConsent"
                type="checkbox"
                checked={form.marketingConsent}
                onChange={(event) => {
                  markTouched('marketingConsent');
                  updateField('marketingConsent', event.target.checked);
                  if (event.target.checked) {
                    trackCompletion('marketingConsent');
                  }
                }}
              />
              <span>Quero receber novidades e conteúdos da Kaza.</span>
            </label>
          </div>
          <p className="help-text">{shouldShowHelp('contactConsent') ? errors.contactConsent : '\u00A0'}</p>
        </div>

        <div className="field field--full">
          <div className="form-actions">
            <button className="button button-primary" type="submit" disabled={status === 'submitting'}>
              {status === 'submitting' ? 'Enviando...' : 'Receber contato'}
            </button>
            <a className="button button-secondary" href="#beneficios">
              Ver benefícios
            </a>
          </div>
        </div>
      </form>

      {feedback ? (
        <div
          className={status === 'success' ? 'alert alert-success' : 'alert alert-error'}
          role="status"
          aria-live="polite"
        >
          {feedback}
        </div>
      ) : null}
    </section>
  );
}
