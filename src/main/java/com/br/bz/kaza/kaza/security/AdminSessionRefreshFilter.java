package com.br.bz.kaza.kaza.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnBean(AdminAuthService.class)
public class AdminSessionRefreshFilter extends OncePerRequestFilter {
    private final AdminAuthService authService;

    AdminSessionRefreshFilter(AdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        authService.refreshSessionIfNeeded(request, response);
        filterChain.doFilter(request, response);
    }
}
