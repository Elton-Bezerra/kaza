package com.br.bz.kaza.kaza.web;

import com.br.bz.kaza.kaza.api.ApiDtos;
import com.br.bz.kaza.kaza.domain.Condominium;
import com.br.bz.kaza.kaza.service.CondominiumService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/condominiums")
public class CondominiumController {

    private final CondominiumService service;

    public CondominiumController(CondominiumService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SINDICO', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.CondominiumResponse create(@RequestBody ApiDtos.CondominiumRequest request, @AuthenticationPrincipal Jwt jwt) {
        Condominium c = service.create(request, jwt);
        return response(c);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MORADOR', 'SINDICO', 'SUPER_ADMIN')")
    public ApiDtos.CondominiumResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return response(service.getForMember(id, jwt));
    }

    private ApiDtos.CondominiumResponse response(Condominium c) {
        return new ApiDtos.CondominiumResponse(c.getId(), c.getName(), c.getTaxId(), c.getStatus().name(), c.fractionTotal());
    }
}
