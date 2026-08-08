# Unit payment preferences

## User perspective

Each resident chooses how their own monthly charge should be paid. One unit can use PIX while another uses boleto or credit card. The choice is not made once for the entire condominium.

Residents reach this flow after accepting a Síndico invitation and completing their Keycloak account setup. The invitation establishes the relationship; the payment preference is a later, changeable domain setting.

Supported methods:

- `PIX`
- `BOLETO`
- `CREDIT_CARD`

`DEBIT_CARD` is rejected because it is not sent by this API integration.

## API call

```http
PUT /api/v1/condominiums/{condominiumId}/units/{unitId}/payment-method
Authorization: ******
```

```json
{
  "billingType": "BOLETO"
}
```

## What happens

Kaza verifies that the authenticated administrator owns the condominium, locates the unit, validates the method against the supported set, and persists it. The setting is used the next time a monthly billing run creates that unit's Asaas payment.

Changing the preference does not recreate an existing charge. It affects future billing runs only. This avoids changing a payment after it has already been presented to a resident.
