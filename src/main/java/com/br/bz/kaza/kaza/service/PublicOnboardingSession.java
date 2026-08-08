package com.br.bz.kaza.kaza.service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public record PublicOnboardingSession(
        UUID invitationId,
        UUID leadId,
        UUID applicationId,
        String subject,
        String email,
        String name,
        Instant createdAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Jwt toJwt() {
        Instant issuedAt = createdAt == null ? Instant.now() : createdAt;
        Instant expiresAt = issuedAt.plusSeconds(7L * 24 * 60 * 60);
        return new Jwt("public-onboarding-session", issuedAt, expiresAt, Map.of("alg", "none"), Map.of(
                "sub", subject,
                "email", email,
                "name", name,
                "aud", List.of("kaza-web")));
    }
}
