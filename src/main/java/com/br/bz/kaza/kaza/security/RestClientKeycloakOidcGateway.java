package com.br.bz.kaza.kaza.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
class RestClientKeycloakOidcGateway implements KeycloakOidcGateway {
    private final RestClient restClient;
    private final AdminAuthProperties properties;

    RestClientKeycloakOidcGateway(RestClient.Builder restClientBuilder, AdminAuthProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public TokenResponse exchangeCode(String code, String codeVerifier, String redirectUri) {
        return postTokenRequest(form -> {
            form.add("grant_type", "authorization_code");
            form.add("code", code);
            form.add("redirect_uri", redirectUri);
            form.add("code_verifier", codeVerifier);
        });
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        return postTokenRequest(form -> {
            form.add("grant_type", "refresh_token");
            form.add("refresh_token", refreshToken);
        });
    }

    @Override
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("refresh_token", refreshToken);
        restClient.post()
                .uri(endpoint("protocol/openid-connect/logout"))
                .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    private TokenResponse postTokenRequest(java.util.function.Consumer<MultiValueMap<String, String>> formConsumer) {
        return postTokenRequest(formConsumer, endpoint("protocol/openid-connect/token"));
    }

    private TokenResponse postTokenRequest(java.util.function.Consumer<MultiValueMap<String, String>> formConsumer,
            URI endpoint) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        formConsumer.accept(form);
        OidcTokenEndpointResponse response = restClient.post()
                .uri(endpoint)
                .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OidcTokenEndpointResponse.class);
        if (response == null) {
            throw new IllegalStateException("Keycloak token response was empty");
        }
        return response.toTokenResponse();
    }

    private URI endpoint(String path) {
        String issuer = properties.issuerUri().toString();
        if (issuer.endsWith("/")) {
            issuer = issuer.substring(0, issuer.length() - 1);
        }
        return URI.create(issuer + "/" + path);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OidcTokenEndpointResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_expires_in") long refreshExpiresIn) {
        TokenResponse toTokenResponse() {
            return new TokenResponse(accessToken, refreshToken, idToken, expiresIn, refreshExpiresIn);
        }
    }
}
