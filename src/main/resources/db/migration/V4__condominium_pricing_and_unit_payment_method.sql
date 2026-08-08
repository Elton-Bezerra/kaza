ALTER TABLE condominiums
    ADD COLUMN subscription_price_per_unit NUMERIC(10,2) NOT NULL DEFAULT 7.00;

ALTER TABLE units
    ADD COLUMN billing_type VARCHAR(32) NOT NULL DEFAULT 'PIX';

ALTER TABLE units
    ADD CONSTRAINT ck_unit_billing_type
    CHECK (billing_type IN ('PIX', 'BOLETO', 'CREDIT_CARD'));
