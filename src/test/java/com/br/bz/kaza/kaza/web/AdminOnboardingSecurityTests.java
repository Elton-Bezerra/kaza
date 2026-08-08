package com.br.bz.kaza.kaza.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.bz.kaza.kaza.api.AdminDtos;
import com.br.bz.kaza.kaza.security.SecurityConfig;
import com.br.bz.kaza.kaza.service.AdminOnboardingService;
import com.br.bz.kaza.kaza.security.AdminAuthService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOnboardingController.class)
@Import(SecurityConfig.class)
class AdminOnboardingSecurityTests {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AdminOnboardingService admin;
    @MockitoBean
    private AdminAuthService adminAuthService;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void adminEndpointsRejectUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/v1/admin/onboarding/leads"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/onboarding/leads/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/onboarding/applications"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/onboarding/applications/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/onboarding/leads/" + UUID.randomUUID() + "/invitation"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointsRejectNonSuperAdminRequests() throws Exception {
        mvc.perform(get("/api/v1/admin/onboarding/leads")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/onboarding/applications")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/onboarding/leads/" + UUID.randomUUID() + "/invitation")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointsAllowSuperAdminRequests() throws Exception {
        UUID leadId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID applicationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(admin.listLeads(any(), any())).thenReturn(Page.empty());
        when(admin.getLead(leadId)).thenReturn(new AdminDtos.LeadResponse(
                leadId,
                "Maria da Silva",
                "maria@example.com",
                "+55 11 99999-9999",
                "SINDICO",
                true,
                false,
                true,
                "web",
                "/",
                "google",
                "cpc",
                "campaign",
                null,
                null,
                "https://example.com",
                OffsetDateTime.parse("2026-08-08T18:09:10Z"),
                List.of(),
                List.of()));
        when(admin.listApplications(any(), any())).thenReturn(Page.empty());
        when(admin.getApplication(applicationId)).thenReturn(new AdminDtos.ApplicationResponse(
                applicationId,
                "UNDER_REVIEW",
                "applicant-subject",
                "applicant@example.com",
                null,
                null,
                "Maria da Silva",
                "maria@example.com",
                "+55 11 99999-9999",
                "52998224725",
                "Condomínio Flores",
                "Rua das Flores, 10",
                "São Paulo",
                "SP",
                "01001000",
                2,
                new BigDecimal("7.00"),
                List.of(new AdminDtos.ApplicationUnitResponse("101", new BigDecimal("0.50000000"))),
                "Need documents",
                null,
                null,
                OffsetDateTime.parse("2026-08-08T18:09:10Z"),
                OffsetDateTime.parse("2026-08-08T18:09:10Z"),
                OffsetDateTime.parse("2026-08-08T18:09:10Z"),
                OffsetDateTime.parse("2026-08-08T18:09:10Z"),
                1L));
        when(admin.inviteLead(leadId)).thenReturn(new AdminDtos.LeadInvitationResponse(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                "PENDING",
                null,
                OffsetDateTime.parse("2026-08-15T18:09:10Z"),
                null,
                OffsetDateTime.parse("2026-08-08T18:09:10Z"),
                OffsetDateTime.parse("2026-08-08T18:09:10Z")));

        mvc.perform(get("/api/v1/admin/onboarding/leads")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/onboarding/leads/{id}", leadId)
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/onboarding/applications")
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/onboarding/applications/{id}", applicationId)
                        .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/onboarding/leads/{id}/invitation", leadId)
                        .with(user("admin").roles("SUPER_ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    void adminEndpointsIgnoreBearerTokensWithoutSession() throws Exception {
        mvc.perform(get("/api/v1/admin/onboarding/leads")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isUnauthorized());
    }
}
