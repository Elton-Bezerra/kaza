package com.br.bz.kaza.kaza.web;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/asaas")
public class WebhookController {

    private final String token;

    public WebhookController(@Value("${asaas.webhook-token:}") String token) {
        this.token = token;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@RequestHeader(value = "asaas-access-token", required = false) String accessToken,
            @RequestBody Map<String, Object> event) {
        if (token.isBlank() || !token.equals(accessToken)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook token");
        }
        // Persist and enqueue this event before returning in the production worker.
    }
}
