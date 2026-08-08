package com.br.bz.kaza.kaza.api;

import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record AdminSessionResponse(
            boolean authenticated,
            String subject,
            String email,
            String name,
            List<String> roles,
            Instant expiresAt,
            String csrfToken) {
    }

    public record AdminAuthChallengeResponse(
            boolean accepted,
            Instant expiresAt) {
    }

    public record CsrfResponse(
            String token,
            String headerName,
            String parameterName) {
    }

    public record AdminAuthChallengeRequest(
            String state,
            String nonce,
            String codeVerifier) {
    }
}
