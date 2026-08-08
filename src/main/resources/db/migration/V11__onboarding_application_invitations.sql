CREATE TABLE onboarding_application_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES onboarding_leads(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    applicant_subject VARCHAR(200) NOT NULL,
    application_id UUID REFERENCES onboarding_applications(id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_onboarding_application_invitations_lead_created_at
    ON onboarding_application_invitations (lead_id, created_at DESC);

CREATE INDEX idx_onboarding_application_invitations_token_hash
    ON onboarding_application_invitations (token_hash);
