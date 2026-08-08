"use client";

import { type ReactNode, useEffect, useState } from 'react';
import { clearAdminAuthTransaction, completeAdminAuthCallback, getAdminAuthEnvironment } from '@/lib/admin-auth';

type CallbackState = 'loading' | 'error';

function CallbackMessage({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <main className="admin-page" aria-labelledby="admin-callback-title">
      <section className="admin-summary" aria-label="Autenticação administrativa">
        <div className="admin-state">
          <strong id="admin-callback-title">{title}</strong>
          <p>{description}</p>
          {action ? <div className="admin-state-actions">{action}</div> : null}
        </div>
      </section>
    </main>
  );
}

export default function AdminAuthCallbackPage() {
  const environment = getAdminAuthEnvironment();
  const [state, setState] = useState<CallbackState>('loading');
  const [errorMessage, setErrorMessage] = useState<string>('Finalizando a autenticação...');

  useEffect(() => {
    let cancelled = false;

    async function finalize() {
      const url = new URL(window.location.href);
      const code = url.searchParams.get('code');
      const stateParam = url.searchParams.get('state');
      const error = url.searchParams.get('error');
      const errorDescription = url.searchParams.get('error_description');

      if (error) {
        clearAdminAuthTransaction();
        if (!cancelled) {
          setState('error');
          setErrorMessage(errorDescription || error || 'A autenticação foi recusada.');
        }
        return;
      }

      if (!code || !stateParam) {
        clearAdminAuthTransaction();
        if (!cancelled) {
          setState('error');
          setErrorMessage('A resposta da autenticação está incompleta.');
        }
        return;
      }

      try {
        await completeAdminAuthCallback(code, stateParam);

        if (cancelled) {
          return;
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        clearAdminAuthTransaction();

        setState('error');
        setErrorMessage(error instanceof Error ? error.message : 'Não foi possível concluir a autenticação.');
      }
    }

    void finalize();

    return () => {
      cancelled = true;
    };
  }, [environment.postLoginPath]);

  if (state === 'error') {
    return (
      <CallbackMessage
        title="Falha na autenticação"
        description={errorMessage}
        action={
          <a className="button button-primary" href={environment.postLoginPath}>
            Voltar ao painel
          </a>
        }
      />
    );
  }

  return <CallbackMessage title="Autenticando" description={errorMessage} />;
}
