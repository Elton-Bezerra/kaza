package com.br.bz.kaza.kaza.asaas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AsaasClient {

    private final RestClient client;
    private final String apiKey;
    private final String platformWalletId;

    public AsaasClient(@Value("${asaas.base-url}") String baseUrl,
            @Value("${asaas.api-key}") String apiKey,
            @Value("${asaas.platform-wallet-id:}") String platformWalletId) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.platformWalletId = platformWalletId;
    }

    public Account createSubaccount(String name, String email, String taxId) {
        requireKey();
        return client.post().uri("/accounts").header("access_token", apiKey)
                .body(Map.of("name", name, "email", email, "cpfCnpj", taxId))
                .retrieve().body(Account.class);
    }

    public Customer createCustomer(String key, String name, String taxId) {
        requireKey(key);
        return client.post().uri("/customers").header("access_token", key)
                .body(Map.of("name", name, "cpfCnpj", taxId))
                .retrieve().body(Customer.class);
    }

    public Payment createPayment(String key, String customerId, BigDecimal value, LocalDate dueDate, String billingType) {
        requireKey(key);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("customer", customerId);
        body.put("billingType", billingType);
        body.put("value", value);
        body.put("dueDate", dueDate.toString());
        if (!platformWalletId.isBlank()) {
            body.put("split", List.of(Map.of("walletId", platformWalletId, "percentualValue", 10)));
        }

        return client.post().uri("/payments").header("access_token", key)
                .body(body).retrieve().body(Payment.class);
    }

    public boolean isConfigured(String key) {
        return key != null && !key.isBlank();
    }

    private void requireKey() {
        requireKey(apiKey);
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Asaas API key is not configured");
        }
    }

    public record Account(String id, String walletId, String apiKey) {

    }

    public record Customer(String id) {

    }

    public record Payment(String id, String status, String invoiceUrl, String bankSlipUrl) {

    }
}
