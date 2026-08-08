CREATE TABLE billing_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL REFERENCES condominiums(id),
    period VARCHAR(7) NOT NULL,
    expenses_total NUMERIC(14,2) NOT NULL CHECK (expenses_total >= 0),
    subscription_total NUMERIC(14,2) NOT NULL CHECK (subscription_total >= 0),
    total NUMERIC(14,2) NOT NULL CHECK (total > 0),
    status VARCHAR(32) NOT NULL,
    UNIQUE (condominium_id, period)
);

ALTER TABLE charges ADD COLUMN billing_run_id UUID REFERENCES billing_runs(id);
ALTER TABLE charges ALTER COLUMN asaas_payment_id DROP NOT NULL;
ALTER TABLE charges ALTER COLUMN billing_run_id SET NOT NULL;
