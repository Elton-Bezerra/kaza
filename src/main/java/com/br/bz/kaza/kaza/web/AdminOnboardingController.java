package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.AdminDtos;
import com.br.bz.kaza.kaza.domain.OnboardingApplicationStatus;
import com.br.bz.kaza.kaza.service.AdminOnboardingService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/onboarding")
public class AdminOnboardingController {
    private final AdminOnboardingService admin;

    public AdminOnboardingController(AdminOnboardingService admin) {
        this.admin = admin;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminDtos.AdminSummaryResponse summary() {
        return admin.summary();
    }

    @GetMapping("/leads")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<AdminDtos.LeadSummaryResponse> listLeads(@RequestParam(required = false) String q,
            Pageable pageable) {
        return admin.listLeads(q, pageable);
    }

    @GetMapping("/leads/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminDtos.LeadResponse getLead(@PathVariable UUID id) {
        return admin.getLead(id);
    }

    @PostMapping("/leads/{id}/invitation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminDtos.LeadInvitationResponse inviteLead(@PathVariable UUID id) {
        return admin.inviteLead(id);
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Page<AdminDtos.ApplicationSummaryResponse> listApplications(
            @RequestParam(required = false) OnboardingApplicationStatus status,
            Pageable pageable) {
        return admin.listApplications(status, pageable);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AdminDtos.ApplicationResponse getApplication(@PathVariable UUID id) {
        return admin.getApplication(id);
    }
}
