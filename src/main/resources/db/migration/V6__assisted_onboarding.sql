CREATE TABLE onboarding_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    declared_role VARCHAR(32) NOT NULL CHECK (declared_role IN ('SINDICO', 'MORADOR', 'OUTRO')),
    utm_source VARCHAR(100),
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(150),
    utm_term VARCHAR(150),
    utm_content VARCHAR(150),
    referrer VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_onboarding_leads_email_created_at
    ON onboarding_leads (LOWER(email), created_at DESC);

CREATE TABLE onboarding_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    applicant_user_id UUID NOT NULL REFERENCES users(id),
    lead_id UUID REFERENCES onboarding_leads(id),
    status VARCHAR(40) NOT NULL CHECK (status IN (
        'DRAFT', 'UNDER_REVIEW', 'NEEDS_MORE_INFORMATION', 'APPROVED',
        'ACTIVATING', 'KAZACONTA_PENDING', 'ACTIVE', 'REJECTED'
    )),
    responsible_name VARCHAR(150),
    responsible_email VARCHAR(255),
    responsible_phone VARCHAR(32),
    tax_id VARCHAR(14),
    condominium_name VARCHAR(255),
    address_line VARCHAR(255),
    address_city VARCHAR(120),
    address_state VARCHAR(2),
    postal_code VARCHAR(8),
    proposed_unit_count INTEGER,
    subscription_price_per_unit NUMERIC(10,2),
    reviewer_user_id UUID REFERENCES users(id),
    review_reason VARCHAR(2000),
    submitted_at TIMESTAMP WITH TIME ZONE,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_onboarding_unit_count CHECK (proposed_unit_count IS NULL OR proposed_unit_count > 0),
    CONSTRAINT ck_onboarding_subscription_price CHECK (
        subscription_price_per_unit IS NULL OR subscription_price_per_unit > 0
    )
);

CREATE INDEX idx_onboarding_applications_applicant
    ON onboarding_applications (applicant_user_id, updated_at DESC);
CREATE INDEX idx_onboarding_applications_status
    ON onboarding_applications (status, submitted_at);

CREATE TABLE onboarding_application_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES onboarding_applications(id) ON DELETE CASCADE,
    identifier VARCHAR(100) NOT NULL,
    ideal_fraction NUMERIC(12,8) NOT NULL CHECK (ideal_fraction > 0 AND ideal_fraction <= 1),
    position INTEGER NOT NULL,
    CONSTRAINT uk_onboarding_unit_identifier UNIQUE (application_id, identifier),
    CONSTRAINT uk_onboarding_unit_position UNIQUE (application_id, position)
);

CREATE TABLE onboarding_document_objects (
    storage_key UUID PRIMARY KEY,
    content BYTEA NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE onboarding_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES onboarding_applications(id) ON DELETE CASCADE,
    storage_key UUID NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    sha256 VARCHAR(64) NOT NULL,
    scan_status VARCHAR(32) NOT NULL CHECK (scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'FAILED')),
    retention_state VARCHAR(32) NOT NULL CHECK (retention_state IN ('ACTIVE', 'DELETED')),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_onboarding_documents_application
    ON onboarding_documents (application_id, retention_state, uploaded_at);
