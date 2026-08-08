package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.ApiDtos;
import com.br.bz.kaza.kaza.domain.Condominium;
import com.br.bz.kaza.kaza.domain.Unit;
import com.br.bz.kaza.kaza.service.CondominiumService;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/condominiums/{condominiumId}/units")
public class UnitController {
    private final CondominiumService condos;
    public UnitController(CondominiumService condos) {
        this.condos = condos;
    }

    @PutMapping("/{unitId}/payment-method")
    @PreAuthorize("hasAnyRole('SINDICO', 'SUPER_ADMIN')")
    public String updatePaymentMethod(@PathVariable UUID condominiumId, @PathVariable UUID unitId,
            @RequestBody ApiDtos.PaymentMethodRequest request, @AuthenticationPrincipal Jwt jwt) {
        Condominium condo = condos.getForSyndic(condominiumId, jwt);
        Unit unit = condo.getUnits().stream().filter(candidate -> candidate.getId().equals(unitId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unit not found"));
        unit.setBillingType(condos.validBillingType(request.billingType()));
        return unit.getBillingType();
    }
}
