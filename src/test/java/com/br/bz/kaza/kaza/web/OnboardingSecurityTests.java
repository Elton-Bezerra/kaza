package com.br.bz.kaza.kaza.web;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.bz.kaza.kaza.security.SecurityConfig;
import com.br.bz.kaza.kaza.service.CurrentUserService;
import com.br.bz.kaza.kaza.service.OnboardingApplicationService;
import com.br.bz.kaza.kaza.service.OnboardingDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
@Import(SecurityConfig.class)
class OnboardingSecurityTests {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CurrentUserService currentUser;
    @MockitoBean
    private OnboardingApplicationService applications;
    @MockitoBean
    private OnboardingDocumentService documents;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void applicantEndpointsRejectUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/onboarding/applications")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
