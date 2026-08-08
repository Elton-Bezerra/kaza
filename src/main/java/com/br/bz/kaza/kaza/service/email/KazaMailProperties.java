package com.br.bz.kaza.kaza.service.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kaza.mail")
public record KazaMailProperties(
        @NotBlank String from,
        @NotNull @Valid Leads leads) {

    public record Leads(
            @NotBlank String internalRecipient,
            @NotBlank String internalSubject,
            @NotBlank String internalBody,
            @NotBlank String prospectSubject,
            @NotBlank String prospectBody,
            @NotBlank String invitationSubject,
            @NotBlank String invitationBody) {
    }
}
