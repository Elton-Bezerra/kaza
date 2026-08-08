package com.br.bz.kaza.kaza.security;

public interface KeycloakOidcGateway {
    TokenResponse exchangeCode(String code, String codeVerifier, String redirectUri);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);

    record TokenResponse(
            String accessToken,
            String refreshToken,
            String idToken,
            long expiresIn,
            long refreshExpiresIn) {
    }
}
