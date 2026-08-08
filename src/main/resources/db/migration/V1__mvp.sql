CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE condominiums (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(14) NOT NULL UNIQUE,
    admin_subject VARCHAR(255) NOT NULL,
    approval_pin_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    asaas_account_id VARCHAR(255),
    asaas_wallet_id VARCHAR(255),
    asaas_api_key TEXT
);

CREATE TABLE units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL REFERENCES condominiums(id),
    identifier VARCHAR(100) NOT NULL,
    ideal_fraction NUMERIC(12,8) NOT NULL CHECK (ideal_fraction > 0 AND ideal_fraction <= 1),
    resident_subject VARCHAR(255),
    resident_name VARCHAR(255),
    resident_tax_id VARCHAR(14),
    asaas_customer_id VARCHAR(255),
    UNIQUE (condominium_id, identifier)
);

CREATE TABLE charges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL REFERENCES condominiums(id),
    unit_id UUID NOT NULL REFERENCES units(id),
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    billing_type VARCHAR(32) NOT NULL,
    asaas_payment_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(64) NOT NULL
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    condominium_id UUID NOT NULL REFERENCES condominiums(id),
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    due_date DATE NOT NULL,
    barcode VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP WITH TIME ZONE,
    asaas_payment_id VARCHAR(255)
);
