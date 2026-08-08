package com.br.bz.kaza.kaza.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.bz.kaza.kaza.security.AdminAuthService;
import com.br.bz.kaza.kaza.security.AdminSessionRefreshFilter;
import com.br.bz.kaza.kaza.security.KeycloakOidcGateway;
import com.br.bz.kaza.kaza.service.AdminOnboardingService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminAuthController.class, AdminOnboardingController.class})
@Import({com.br.bz.kaza.kaza.security.SecurityConfig.class, AdminAuthService.class, AdminSessionRefreshFilter.class})
@TestPropertySource(properties = {
        "kaza.admin.auth.issuer-uri=http://localhost:8081/realms/kaza",
        "kaza.admin.auth.client-id=kaza-admin-bff",
        "kaza.admin.auth.client-secret=kaza-admin-bff-local-secret",
        "kaza.admin.auth.redirect-uri=http://localhost:8080/api/v1/admin/auth/callback",
        "kaza.admin.auth.post-login-redirect-uri=http://localhost:3000/admin",
        "kaza.admin.auth.post-logout-redirect-uri=http://localhost:3000/admin",
        "kaza.admin.auth.session-timeout=PT30M",
        "kaza.admin.auth.refresh-window=PT2M",
        "kaza.admin.auth.challenge-timeout=PT5M",
        "kaza.admin.auth.allowed-origins=http://localhost:3000"
})
class AdminAuthControllerTests {
    private static final Instant NOW = Instant.parse("2026-08-08T18:15:54Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private KeycloakOidcGateway gateway;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private AdminOnboardingService adminOnboardingService;

    @Test
    void loginCreatesChallengeAndRedirectsToKeycloak() throws Exception {
        var result = mvc.perform(get("/api/v1/admin/auth/login"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(
                        "http://localhost:8081/realms/kaza/protocol/openid-connect/auth")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("client_id=kaza-admin-bff")))
                .andReturn();

        Object challenge = result.getRequest().getSession(false).getAttribute(AdminAuthService.CHALLENGE_SESSION_ATTRIBUTE);
        assertThat(readChallenge(challenge, "state")).isNotBlank();
        assertThat(readChallenge(challenge, "nonce")).isNotBlank();
        assertThat(readChallenge(challenge, "codeVerifier")).isNotBlank();
    }

    @Test
    void loginResponseUsesSecureSessionCookieFlagsWhereAvailable() throws Exception {
        var response = mvc.perform(get("/api/v1/admin/auth/login"))
                .andExpect(status().isSeeOther())
                .andReturn().getResponse();

        String setCookie = response.getHeader("Set-Cookie");
        if (setCookie != null) {
            assertThat(setCookie).contains("HttpOnly").contains("Secure");
            assertThat(setCookie).containsIgnoringCase("SameSite=Lax");
        }
    }

    @Test
    void callbackRejectsBadState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = "statevalue123456";
        String nonce = "noncevalue123456";
        String verifier = "verifiervalue123456";
        mvc.perform(post("/api/v1/admin/auth/challenge")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"%s","nonce":"%s","codeVerifier":"%s"}
                                """.formatted(state, nonce, verifier)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/auth/callback")
                        .session(session)
                        .param("code", "code")
                        .param("state", "wrong-state"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void callbackRejectsNonSuperAdminUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = "statevalue123456";
        String nonce = "noncevalue123456";
        String verifier = "verifiervalue123456";
        mvc.perform(post("/api/v1/admin/auth/challenge")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"%s","nonce":"%s","codeVerifier":"%s"}
                                """.formatted(state, nonce, verifier)))
                .andExpect(status().isOk());

        when(gateway.exchangeCode("code", verifier, "http://localhost:8080/api/v1/admin/auth/callback"))
                .thenReturn(new KeycloakOidcGateway.TokenResponse("access-token", "refresh-token", "id-token", 300, 1200));
        when(jwtDecoder.decode("id-token")).thenReturn(jwt("id-token", Map.of(
                "sub", "subject",
                "aud", List.of("kaza-admin-bff"),
                "nonce", nonce,
                "email", "admin@example.com",
                "preferred_username", "admin",
                "name", "Admin")));
        when(jwtDecoder.decode("access-token")).thenReturn(jwt("access-token", Map.of(
                "sub", "subject",
                "aud", List.of("kaza-admin-bff"),
                "azp", "kaza-admin-bff",
                "realm_access", Map.of("roles", List.of("USER")))));

        mvc.perform(get("/api/v1/admin/auth/callback")
                        .session(session)
                        .param("code", "code")
                        .param("state", state))
                .andExpect(status().isForbidden());
    }

    @Test
    void successfulFrontendInitiatedSessionSupportsLogoutAndSessionInspection() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = "statevalue123456";
        String nonce = "noncevalue123456";
        String verifier = "verifiervalue123456";
        mvc.perform(post("/api/v1/admin/auth/challenge")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"%s","nonce":"%s","codeVerifier":"%s"}
                                """.formatted(state, nonce, verifier)))
                .andExpect(status().isOk());

        when(gateway.exchangeCode("code", verifier, "http://localhost:8080/api/v1/admin/auth/callback"))
                .thenReturn(new KeycloakOidcGateway.TokenResponse("access-token", "refresh-token", "id-token", 300, 1200));
        when(jwtDecoder.decode("id-token")).thenReturn(jwt("id-token", Map.of(
                "sub", "subject",
                "aud", List.of("kaza-admin-bff"),
                "nonce", nonce,
                "email", "admin@example.com",
                "preferred_username", "admin",
                "name", "Admin")));
        when(jwtDecoder.decode("access-token")).thenReturn(jwt("access-token", Map.of(
                "sub", "subject",
                "aud", List.of("kaza-admin-bff"),
                "azp", "kaza-admin-bff",
                "realm_access", Map.of("roles", List.of("SUPER_ADMIN")))));

        mvc.perform(get("/api/v1/admin/auth/callback")
                        .session(session)
                        .param("code", "code")
                        .param("state", state))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "http://localhost:3000/admin"));

        mvc.perform(get("/api/v1/admin/auth/session")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.csrfToken").exists());

        mvc.perform(post("/api/v1/admin/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "http://localhost:3000/admin"));

        mvc.perform(get("/api/v1/admin/auth/session")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void challengeEndpointRequiresCsrfButReturnsAcceptedTransaction() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/challenge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"statevalue123456","nonce":"noncevalue123456","codeVerifier":"verifiervalue123456"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/admin/auth/challenge")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"state":"statevalue123456","nonce":"noncevalue123456","codeVerifier":"verifiervalue123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void csrfEndpointReturnsTokenMetadata() throws Exception {
        mvc.perform(get("/api/v1/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.headerName").exists())
                .andExpect(jsonPath("$.parameterName").exists());
    }

    @Test
    void adminEndpointsRejectUnauthenticatedAndNonSuperAdminRequests() throws Exception {
        mvc.perform(get("/api/v1/admin/onboarding/leads"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/onboarding/leads")
                        .with(user("admin").roles("USER")))
                .andExpect(status().isForbidden());
    }

    private Jwt jwt(String tokenValue, Map<String, Object> claims) {
        return new Jwt(tokenValue, NOW, NOW.plusSeconds(300), Map.of("alg", "none"), claims);
    }

    private String readChallenge(Object challenge, String accessor) throws Exception {
        if (challenge == null) {
            return null;
        }
        String methodName = switch (accessor) {
            case "state" -> "state";
            case "nonce" -> "nonce";
            case "codeVerifier" -> "codeVerifier";
            default -> throw new IllegalArgumentException("Unknown accessor: " + accessor);
        };
        Method method = challenge.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(challenge);
    }
}
