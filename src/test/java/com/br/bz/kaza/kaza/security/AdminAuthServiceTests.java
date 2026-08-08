package com.br.bz.kaza.kaza.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import com.br.bz.kaza.kaza.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTests {
    private static final Instant NOW = Instant.parse("2026-08-08T18:15:54Z");

    @Mock
    private KeycloakOidcGateway gateway;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private SecurityContextRepository securityContextRepository;
    @Mock
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;
    @Mock
    private UserService users;

    private AdminAuthService service;

    @BeforeEach
    void setUp() {
        AdminAuthProperties properties = new AdminAuthProperties(
                java.net.URI.create("http://localhost:8081/realms/kaza"),
                "kaza-admin-bff",
                "kaza-admin-bff-local-secret",
                java.net.URI.create("http://localhost:8080/api/v1/admin/auth/callback"),
                java.net.URI.create("http://localhost:3000/admin"),
                java.net.URI.create("http://localhost:3000/admin"),
                Duration.ofMinutes(30),
                Duration.ofMinutes(2),
                Duration.ofMinutes(5),
                List.of("http://localhost:3000"));
        service = new AdminAuthService(properties, gateway, jwtDecoder, securityContextRepository,
                sessionAuthenticationStrategy, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void beginLoginStoresStateNonceAndPkce() {
        MockHttpSession session = new MockHttpSession();

        String redirect = service.beginLogin(session, null);

        assertThat(redirect).contains("response_type=code");
        assertThat(redirect).contains("client_id=kaza-admin-bff");
        assertThat(redirect).contains("redirect_uri=http://localhost:8080/api/v1/admin/auth/callback");
        assertThat(redirect).contains("code_challenge_method=S256");
        assertThat(session.getAttribute(AdminAuthService.CHALLENGE_SESSION_ATTRIBUTE))
                .isInstanceOf(AdminLoginChallenge.class);
    }

    @Test
    void registerChallengeStoresBrowserProvidedStateNonceAndVerifier() {
        MockHttpSession session = new MockHttpSession();

        var response = service.registerChallenge(session, "statevalue123456", "noncevalue123456", "verifiervalue123456");

        assertThat(response.accepted()).isTrue();
        assertThat(response.expiresAt()).isNotNull();
        assertThat(session.getAttribute(AdminAuthService.CHALLENGE_SESSION_ATTRIBUTE))
                .isInstanceOf(AdminLoginChallenge.class);
    }

    @Test
    void completeLoginRejectsStateMismatch() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthService.CHALLENGE_SESSION_ATTRIBUTE,
                new AdminLoginChallenge("expected-state", "nonce", "verifier", NOW));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        HttpServletResponse response = new MockHttpServletResponse();

        Throwable thrown = catchThrowable(() -> service.completeLogin(request, response, "code", "wrong-state"));
        assertThat(thrown).isInstanceOf(AdminAuthHttpException.class);
        assertThat(((AdminAuthHttpException) thrown).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void completeLoginRejectsNonSuperAdminUsers() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthService.CHALLENGE_SESSION_ATTRIBUTE,
                new AdminLoginChallenge("expected-state", "nonce-value", "verifier", NOW));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(gateway.exchangeCode("code", "verifier", "http://localhost:8080/api/v1/admin/auth/callback"))
                .thenReturn(new KeycloakOidcGateway.TokenResponse("access-token", "refresh-token", "id-token", 300, 1200));
        when(jwtDecoder.decode("id-token")).thenReturn(jwt("id-token", Map.of(
                "sub", "subject",
                "aud", List.of("kaza-admin-bff"),
                "nonce", "nonce-value",
                "email", "admin@example.com",
                "preferred_username", "admin",
                "name", "Admin")));
        when(jwtDecoder.decode("access-token")).thenReturn(jwt("access-token", Map.of(
                "sub", "subject",
                "aud", List.of("kaza-admin-bff"),
                "azp", "kaza-admin-bff",
                "realm_access", Map.of("roles", List.of("USER")))));

        Throwable thrown = catchThrowable(() -> service.completeLogin(request, response, "code", "expected-state"));
        assertThat(thrown).isInstanceOf(AdminAuthHttpException.class);
        assertThat(((AdminAuthHttpException) thrown).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(gateway).exchangeCode("code", "verifier", "http://localhost:8080/api/v1/admin/auth/callback");
    }

    @Test
    void logoutRevokesRefreshTokenAndInvalidatesSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthService.SESSION_STATE_ATTRIBUTE, new AdminSessionState(
                new AdminSessionPrincipal("subject", "admin@example.com", "Admin", List.of("SUPER_ADMIN")),
                "access-token",
                "refresh-token",
                "id-token",
                NOW.plusSeconds(300),
                NOW.plusSeconds(1200),
                NOW));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.logout(request, response);

        verify(gateway).logout("refresh-token");
        assertThat(request.getSession(false)).isNull();
    }

    private Jwt jwt(String tokenValue, Map<String, Object> claims) {
        return new Jwt(tokenValue, NOW, NOW.plusSeconds(300), Map.of("alg", "none"), claims);
    }
}
