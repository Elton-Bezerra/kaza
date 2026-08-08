package com.br.bz.kaza.kaza.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kaza.admin.auth")
public record AdminAuthProperties(
        @NotNull URI issuerUri,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotNull URI redirectUri,
        @NotNull URI postLoginRedirectUri,
        @NotNull URI postLogoutRedirectUri,
        @NotNull Duration sessionTimeout,
        @NotNull Duration refreshWindow,
        @NotNull Duration challengeTimeout,
        List<String> allowedOrigins) {
}
