ALTER TABLE onboarding_leads
    ADD COLUMN contact_consent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN analytics_consent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN source VARCHAR(100),
    ADD COLUMN landing_path VARCHAR(500);

ALTER TABLE onboarding_leads
    ALTER COLUMN contact_consent DROP DEFAULT,
    ALTER COLUMN marketing_consent DROP DEFAULT,
    ALTER COLUMN analytics_consent DROP DEFAULT;
