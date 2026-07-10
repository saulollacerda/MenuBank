package com.MenuBank.MenuBank.integration.abacatepay;

import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutData;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutRequest;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutResponse;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayProductRequest;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayProductResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AbacatePayClient {

    private final RestClient restClient;

    public AbacatePayClient(
            RestClient.Builder builder,
            @Value("${abacatepay.base-url}") String baseUrl,
            @Value("${abacatepay.api-key}") String apiKey) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public String createProduct(AbacatePayProductRequest request) {
        AbacatePayProductResponse response = restClient.post()
                .uri("/products/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AbacatePayProductResponse.class);

        if (response == null || response.getError() != null || response.getData() == null) {
            throw new AbacatePayException(errorMessage(response == null ? null : response.getError()));
        }
        return response.getData().getId();
    }

    public AbacatePayCheckoutData createCheckout(AbacatePayCheckoutRequest request) {
        AbacatePayCheckoutResponse response = restClient.post()
                .uri("/checkouts/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AbacatePayCheckoutResponse.class);

        if (response == null || response.getError() != null || response.getData() == null) {
            throw new AbacatePayException(errorMessage(response == null ? null : response.getError()));
        }
        return response.getData();
    }

    private String errorMessage(String error) {
        return "AbacatePay API error: " + (error != null ? error : "empty response");
    }
}
