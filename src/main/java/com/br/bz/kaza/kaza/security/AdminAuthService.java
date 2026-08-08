package com.br.bz.kaza.kaza.security;

import com.br.bz.kaza.kaza.api.AuthDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import com.br.bz.kaza.kaza.service.UserService;

@Service
public class AdminAuthService {
    public static final String CHALLENGE_SESSION_ATTRIBUTE = AdminAuthService.class.getName() + ".CHALLENGE";
    public static final String SESSION_STATE_ATTRIBUTE = AdminAuthService.class.getName() + ".SESSION";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminAuthProperties properties;
    private final KeycloakOidcGateway gateway;
    private final JwtDecoder jwtDecoder;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final UserService users;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    AdminAuthService(AdminAuthProperties properties, KeycloakOidcGateway gateway, JwtDecoder jwtDecoder,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy, UserService users, Clock clock) {
        this.properties = properties;
        this.gateway = gateway;
        this.jwtDecoder = jwtDecoder;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.users = users;
        this.clock = clock;
    }

    public String beginLogin(HttpSession session, Authentication currentAuthentication) {
        if (isAuthenticatedSuperAdmin(currentAuthentication)) {
            return properties.postLoginRedirectUri().toString();
        }
        AdminLoginChallenge challenge = newChallenge();
        storeChallenge(session, challenge);
        return authorizationEndpoint(challenge);
    }

    public AuthDtos.AdminAuthChallengeResponse registerChallenge(HttpSession session, String state, String nonce,
            String codeVerifier) {
        validateChallengeInputs(state, nonce, codeVerifier);
        AdminLoginChallenge challenge = new AdminLoginChallenge(state, nonce, codeVerifier, Instant.now(clock));
        storeChallenge(session, challenge);
        return new AuthDtos.AdminAuthChallengeResponse(true,
                challenge.createdAt().plus(properties.challengeTimeout()));
    }

    public void completeLogin(HttpServletRequest request, HttpServletResponse response, String code, String state) {
        HttpSession session = requireSession(request);
        AdminLoginChallenge challenge = requireChallenge(session);
        if (!Objects.equals(challenge.state(), state)) {
            clearSession(request, response);
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
        }
        if (challengeExpired(challenge)) {
            clearSession(request, response);
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
        }

        try {
            session.removeAttribute(CHALLENGE_SESSION_ATTRIBUTE);
            KeycloakOidcGateway.TokenResponse tokenResponse = gateway.exchangeCode(code, challenge.codeVerifier(),
                    properties.redirectUri().toString());
            Jwt idToken = jwtDecoder.decode(tokenResponse.idToken());
            validateTokenAudience(idToken);
            if (!Objects.equals(idToken.getClaimAsString("nonce"), challenge.nonce())) {
                throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
            }

            Jwt accessToken = jwtDecoder.decode(tokenResponse.accessToken());
            validateTokenAudience(accessToken);
            AdminSessionPrincipal principal = principalFromTokens(idToken, accessToken);
            if (!principal.roles().contains(SUPER_ADMIN)) {
                throw new AdminAuthHttpException(HttpStatus.FORBIDDEN);
            }
            users.ensureFromIdentity(principal.subject(), principal.email(), principal.name());

            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                    principal.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            securityContextRepository.saveContext(securityContext, request, response);

            AdminSessionState stateRecord = new AdminSessionState(principal, tokenResponse.accessToken(),
                    coalesce(tokenResponse.refreshToken()), tokenResponse.idToken(),
                    instantFromExpiresIn(tokenResponse.expiresIn()),
                    refreshExpiresAt(tokenResponse.refreshExpiresIn()), Instant.now(clock));
            request.getSession(true).setAttribute(SESSION_STATE_ATTRIBUTE, stateRecord);
            request.getSession(true).setMaxInactiveInterval((int) properties.sessionTimeout().toSeconds());
        } catch (AdminAuthHttpException ex) {
            clearSession(request, response);
            throw ex;
        } catch (RuntimeException ex) {
            clearSession(request, response);
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED, ex);
        }
    }

    public AuthDtos.AdminSessionResponse currentSession(Authentication authentication, HttpServletRequest request,
            String csrfToken) {
        HttpSession session = request.getSession(false);
        AdminSessionState state = session == null ? null : (AdminSessionState) session.getAttribute(SESSION_STATE_ATTRIBUTE);
        if (state != null) {
            return new AuthDtos.AdminSessionResponse(true, state.principal().subject(), state.principal().email(),
                    state.principal().name(), state.roles(), state.accessTokenExpiresAt(), csrfToken);
        }
        throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        AdminSessionState state = (AdminSessionState) session.getAttribute(SESSION_STATE_ATTRIBUTE);
        if (state != null && StringUtils.hasText(state.refreshToken())) {
            try {
                gateway.logout(state.refreshToken());
            } catch (RuntimeException ignored) {
                // fail closed by invalidating the local session.
            }
        }
        clearSession(request, response);
    }

    public void refreshSessionIfNeeded(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        AdminSessionState state = (AdminSessionState) session.getAttribute(SESSION_STATE_ATTRIBUTE);
        if (state == null) {
            return;
        }
        if (state.refreshTokenExpiresAt() != null && state.refreshTokenExpiresAt().isBefore(Instant.now(clock))) {
            clearSession(request, response);
            return;
        }
        if (state.accessTokenExpiresAt() != null
                && state.accessTokenExpiresAt().isAfter(Instant.now(clock).plus(properties.refreshWindow()))) {
            return;
        }
        if (!StringUtils.hasText(state.refreshToken())) {
            clearSession(request, response);
            return;
        }
        try {
            KeycloakOidcGateway.TokenResponse refreshed = gateway.refresh(state.refreshToken());
            Jwt accessToken = jwtDecoder.decode(refreshed.accessToken());
            validateTokenAudience(accessToken);
            AdminSessionPrincipal principal = principalFromTokens(null, accessToken, state.principal());
            if (!principal.roles().contains(SUPER_ADMIN)) {
                clearSession(request, response);
                return;
            }
            AdminSessionState updated = state.withTokens(refreshed.accessToken(), coalesce(refreshed.refreshToken()),
                    refreshed.idToken(), instantFromExpiresIn(refreshed.expiresIn()),
                    refreshExpiresAt(refreshed.refreshExpiresIn()), Instant.now(clock));
            session.setAttribute(SESSION_STATE_ATTRIBUTE, updated);
            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                    principal.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            securityContextRepository.saveContext(securityContext, request, response);
        } catch (RuntimeException ex) {
            clearSession(request, response);
        }
    }

    private void clearSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private boolean isAuthenticatedSuperAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authorities(authentication).contains(SUPER_ADMIN);
    }

    private List<String> authorities(Authentication authentication) {
        List<String> roles = new ArrayList<>();
        for (var authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role != null && role.startsWith("ROLE_")) {
                roles.add(role.substring("ROLE_".length()));
            }
        }
        return roles;
    }

    private String authorizationEndpoint(AdminLoginChallenge challenge) {
        return UriComponentsBuilder.fromUriString(endpoint("protocol/openid-connect/auth"))
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri().toString())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("state", challenge.state())
                .queryParam("nonce", challenge.nonce())
                .queryParam("code_challenge", codeChallenge(challenge.codeVerifier()))
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();
    }

    private String endpoint(String path) {
        String issuer = properties.issuerUri().toString();
        if (issuer.endsWith("/")) {
            issuer = issuer.substring(0, issuer.length() - 1);
        }
        return issuer + "/" + path;
    }

    private AdminLoginChallenge newChallenge() {
        return new AdminLoginChallenge(randomUrlSafe(), randomUrlSafe(), randomUrlSafe(), Instant.now(clock));
    }

    private void storeChallenge(HttpSession session, AdminLoginChallenge challenge) {
        session.setAttribute(CHALLENGE_SESSION_ATTRIBUTE, challenge);
        session.setMaxInactiveInterval((int) properties.sessionTimeout().toSeconds());
    }

    private String randomUrlSafe() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String codeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private AdminLoginChallenge requireChallenge(HttpSession session) {
        Object value = session.getAttribute(CHALLENGE_SESSION_ATTRIBUTE);
        if (value instanceof AdminLoginChallenge challenge) {
            return challenge;
        }
        throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
    }

    private boolean challengeExpired(AdminLoginChallenge challenge) {
        return challenge.createdAt().plus(properties.challengeTimeout()).isBefore(Instant.now(clock));
    }

    private void validateChallengeInputs(String state, String nonce, String codeVerifier) {
        if (!looksLikeBase64Url(state) || !looksLikeBase64Url(nonce) || !looksLikeBase64Url(codeVerifier)) {
            throw new AdminAuthHttpException(HttpStatus.BAD_REQUEST);
        }
    }

    private boolean looksLikeBase64Url(String value) {
        return value != null && value.length() >= 16 && value.length() <= 256 && value.matches("^[A-Za-z0-9_-]+$");
    }

    private void validateTokenAudience(Jwt token) {
        boolean audienceMatches = token.getAudience() != null && token.getAudience().contains(properties.clientId());
        boolean authorizedPartyMatches = properties.clientId().equals(token.getClaimAsString("azp"));
        if (!audienceMatches && !authorizedPartyMatches) {
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
        }
    }

    private HttpSession requireSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
        }
        return session;
    }

    private AdminSessionPrincipal principalFromTokens(Jwt idToken, Jwt accessToken) {
        return principalFromTokens(idToken, accessToken, null);
    }

    private AdminSessionPrincipal principalFromTokens(Jwt idToken, Jwt accessToken, AdminSessionPrincipal fallback) {
        String fallbackSubject = fallback == null ? null : fallback.subject();
        String fallbackEmail = fallback == null ? null : fallback.email();
        String fallbackName = fallback == null ? null : fallback.name();
        String subject = idToken == null ? fallbackSubject : idToken.getSubject();
        String email = idToken == null ? fallbackEmail : firstNonBlank(idToken.getClaimAsString("email"),
                fallbackEmail);
        String name = idToken == null ? fallbackName : firstNonBlank(idToken.getClaimAsString("name"),
                idToken.getClaimAsString("preferred_username"), fallbackName);
        Set<String> roles = extractRoles(accessToken);
        if (roles.isEmpty() && fallback != null) {
            roles = new LinkedHashSet<>(fallback.roles());
        }
        return new AdminSessionPrincipal(subject, email, name, List.copyOf(roles));
    }

    private Set<String> extractRoles(Jwt accessToken) {
        Set<String> roles = new LinkedHashSet<>();
        Object realmAccess = accessToken.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> claims && claims.get("roles") instanceof Collection<?> realmRoles) {
            for (Object role : realmRoles) {
                if (role != null) {
                    roles.add(role.toString());
                }
            }
        }
        Object resourceAccess = accessToken.getClaims().get("resource_access");
        if (resourceAccess instanceof Map<?, ?> clientAccess) {
            Object client = clientAccess.get(properties.clientId());
            if (client instanceof Map<?, ?> claims && claims.get("roles") instanceof Collection<?> clientRoles) {
                for (Object role : clientRoles) {
                    if (role != null) {
                        roles.add(role.toString());
                    }
                }
            }
        }
        return roles;
    }

    private Instant instantFromExpiresIn(long expiresIn) {
        return expiresIn > 0 ? Instant.now(clock).plusSeconds(expiresIn) : null;
    }

    private Instant refreshExpiresAt(long expiresIn) {
        return expiresIn > 0 ? Instant.now(clock).plusSeconds(expiresIn) : null;
    }

    private String coalesce(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
