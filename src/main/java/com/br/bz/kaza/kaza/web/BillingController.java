package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.ApiDtos;
import com.br.bz.kaza.kaza.domain.BillingRun;
import com.br.bz.kaza.kaza.domain.Condominium;
import com.br.bz.kaza.kaza.service.BillingService;
import com.br.bz.kaza.kaza.service.CondominiumService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/condominiums/{condominiumId}/billing-runs")
public class BillingController {
    private final CondominiumService condos;
    private final BillingService billing;

    public BillingController(CondominiumService condos, BillingService billing) {
        this.condos = condos;
        this.billing = billing;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SINDICO', 'SUPER_ADMIN')")
    public ApiDtos.BillingRunResponse create(@PathVariable UUID condominiumId,
            @RequestBody ApiDtos.BillingRunRequest request, @AuthenticationPrincipal Jwt jwt) {
        Condominium condo = condos.getForSyndic(condominiumId, jwt);
        BillingRun run = billing.create(condo, request);
        return new ApiDtos.BillingRunResponse(run.getId(), run.getPeriod(), run.getExpensesTotal(),
                run.getSubscriptionTotal(), run.getTotal(), run.getStatus().name(), run.getCharges().size());
    }
}
