package com.br.bz.kaza.kaza.security;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AdminAuthProperties.class)
public class SecurityConfig {

    @Value("${kaza.web.allowed-origins:http://localhost:3000,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
        repository.setDisableUrlRewriting(true);
        return repository;
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
            ObjectProvider<AdminSessionRefreshFilter> adminSessionRefreshFilter,
            SecurityContextRepository securityContextRepository,
            AdminAuthProperties adminAuthProperties) throws Exception {
        var security = http.securityMatcher("/api/v1/admin/**")
                .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .cors(cors -> cors.configurationSource(corsConfigurationSource(adminAuthProperties)))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> writeJson(response,
                                HttpStatus.UNAUTHORIZED, "unauthorized"))
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/api/v1/admin/**").permitAll()
                        .requestMatchers("/api/v1/admin/auth/login", "/api/v1/admin/auth/callback",
                                "/api/v1/admin/auth/challenge", "/api/v1/admin/auth/csrf").permitAll()
                        .anyRequest().hasRole("SUPER_ADMIN"))
                ;
        AdminSessionRefreshFilter refreshFilter = adminSessionRefreshFilter.getIfAvailable();
        if (refreshFilter != null) {
            security.addFilterBefore(refreshFilter,
                    org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        }
        return security.build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, AdminAuthProperties adminAuthProperties) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(adminAuthProperties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/v1/webhooks/asaas",
                                "/api/v1/onboarding/leads", "/api/v1/public/landing-events",
                                "/api/v1/public/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AdminAuthProperties adminAuthProperties) {
        List<String> origins = sanitizedAllowedOrigins(
                adminAuthProperties.allowedOrigins() == null || adminAuthProperties.allowedOrigins().isEmpty()
                        ? allowedOrigins
                        : adminAuthProperties.allowedOrigins());
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN", "X-XSRF-TOKEN",
                "X-Requested-With"));
        configuration.setAllowCredentials(!origins.isEmpty());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> sanitizedAllowedOrigins(List<String> originsInput) {
        if (originsInput == null) {
            return List.of();
        }
        List<String> origins = originsInput.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::trim)
                .toList();
        if (origins.stream().anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException("Wildcard CORS origins are not allowed");
        }
        return origins;
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> claims && claims.get("roles") instanceof Collection<?> roles) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeJson(response, HttpStatus.FORBIDDEN, "forbidden");
    }

    private void writeJson(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String error)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write("{\"error\":\"" + error + "\"}");
    }
}
