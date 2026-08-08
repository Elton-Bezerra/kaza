CREATE TABLE lead_email_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES onboarding_leads(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_lead_email_outbox_lead_status_created_at
    ON lead_email_outbox (lead_id, status, created_at);

CREATE INDEX idx_onboarding_leads_created_at
    ON onboarding_leads (created_at DESC);

CREATE INDEX idx_onboarding_applications_created_at
    ON onboarding_applications (created_at DESC);
