# Public landing page contract

Este documento define a fronteira entre o frontend público da Kaza e o backend.

## 1. Lead capture

```http
POST /api/v1/onboarding/leads
Content-Type: application/json
```

### Payload mínimo

```json
{
  "name": "Nome completo",
  "email": "contato@exemplo.com",
  "phone": "+55 11 99999-9999",
  "role": "SINDICO",
  "contactConsent": true,
  "marketingConsent": false,
  "analyticsConsent": false,
  "source": "web-landing",
  "attribution": {
    "landingPath": "/?utm_source=instagram",
    "referrer": "https://exemplo.com/artigo",
    "utmSource": "instagram",
    "utmMedium": null,
    "utmCampaign": null,
    "utmContent": null,
    "utmTerm": null
  }
}
```

### Regras

- O backend valida os quatro campos obrigatórios: `name`, `email`, `phone`, `role`.
- O consentimento de contato é necessário para triagem comercial.
- `marketingConsent` é opcional.
- `analyticsConsent` serve apenas para auditoria do funil.
- `attribution` é opcional no backend, mas o frontend tenta enviá-lo quando disponível.
- O backend pode responder `201 Created`, `202 Accepted` ou `204 No Content`.

## 2. Analytics anônimo do funil

```http
POST /api/v1/public/landing-events
Content-Type: application/json
```

### Eventos esperados

- `page_view`
- `cta_view`
- `cta_click`
- `form_start`
- `field_complete`
- `submit_success`
- `submit_error`

### Princípios

- Não usar fingerprinting.
- Não registrar teclas, campos ocultos ou dados sensíveis extra.
- Não criar identificadores persistentes no navegador.
- Respeitar a preferência de consentimento do visitante.
- Aceitar eventos anônimos e idempotentes por ocorrência.
