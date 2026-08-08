CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    display_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE condominium_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    condominium_id UUID NOT NULL REFERENCES condominiums(id),
    role VARCHAR(32) NOT NULL CHECK (role IN ('SINDICO', 'MORADOR')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_membership_user_condominium UNIQUE (user_id, condominium_id)
);

INSERT INTO users (subject, created_at, updated_at)
SELECT DISTINCT admin_subject, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM condominiums
WHERE admin_subject IS NOT NULL AND admin_subject <> '';

INSERT INTO condominium_memberships (user_id, condominium_id, role, created_at, updated_at)
SELECT u.id, c.id, 'SINDICO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM condominiums c
JOIN users u ON u.subject = c.admin_subject;
