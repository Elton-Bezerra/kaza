package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.AuthDtos;
import com.br.bz.kaza.kaza.security.AdminAuthHttpException;
import com.br.bz.kaza.kaza.security.AdminAuthProperties;
import com.br.bz.kaza.kaza.security.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminAuthService auth;
    private final AdminAuthProperties properties;

    public AdminAuthController(AdminAuthService auth, AdminAuthProperties properties) {
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpSession session, Authentication authentication) {
        URI location = URI.create(auth.beginLogin(session, authentication));
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(location)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<AuthDtos.CsrfResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(new AuthDtos.CsrfResponse(csrfToken.getToken(), csrfToken.getHeaderName(),
                        csrfToken.getParameterName()));
    }

    @PostMapping("/challenge")
    public ResponseEntity<AuthDtos.AdminAuthChallengeResponse> challenge(HttpSession session,
            @RequestBody AuthDtos.AdminAuthChallengeRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(auth.registerChallenge(session, request.state(), request.nonce(), request.codeVerifier()));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(required = false) String code, @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null && !error.isBlank()) {
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
        }
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            throw new AdminAuthHttpException(HttpStatus.UNAUTHORIZED);
        }
        auth.completeLogin(request, response, code, state);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(properties.postLoginRedirectUri())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        auth.logout(request, response);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(properties.postLogoutRedirectUri())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }

    @GetMapping("/session")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AuthDtos.AdminSessionResponse session(Authentication authentication, HttpServletRequest request,
            CsrfToken csrfToken) {
        return auth.currentSession(authentication, request, csrfToken == null ? null : csrfToken.getToken());
    }
}
