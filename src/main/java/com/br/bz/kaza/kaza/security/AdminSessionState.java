package com.br.bz.kaza.kaza.security;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

record AdminSessionState(
        AdminSessionPrincipal principal,
        String accessToken,
        String refreshToken,
        String idToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        Instant authenticatedAt) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    AdminSessionState withTokens(String accessToken, String refreshToken, String idToken, Instant accessTokenExpiresAt,
            Instant refreshTokenExpiresAt, Instant authenticatedAt) {
        return new AdminSessionState(principal, accessToken, refreshToken, idToken, accessTokenExpiresAt,
                refreshTokenExpiresAt, authenticatedAt);
    }

    List<String> roles() {
        return principal.roles();
    }
}
