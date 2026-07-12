package com.MenuBank.MenuBank.integration.abacatepay;

import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutData;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutRequest;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutResponse;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayProductRequest;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AbacatePayClient {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayClient.class);

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
        AbacatePayProductResponse response = post("/products/create", request, AbacatePayProductResponse.class);

        if (response == null || response.getError() != null || response.getData() == null) {
            throw new AbacatePayException(errorMessage(response == null ? null : response.getError()));
        }
        return response.getData().getId();
    }

    public AbacatePayCheckoutData createCheckout(AbacatePayCheckoutRequest request) {
        AbacatePayCheckoutResponse response = post("/checkouts/create", request, AbacatePayCheckoutResponse.class);

        if (response == null || response.getError() != null || response.getData() == null) {
            throw new AbacatePayException(errorMessage(response == null ? null : response.getError()));
        }
        return response.getData();
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException ex) {
            log.error("AbacatePay call to {} failed with HTTP {}: {}",
                    uri, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new AbacatePayException(
                    "AbacatePay call to " + uri + " failed: HTTP " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            log.error("AbacatePay call to {} failed: {}", uri, ex.getMessage());
            throw new AbacatePayException("AbacatePay call to " + uri + " failed: " + ex.getMessage(), ex);
        }
    }

    private String errorMessage(String error) {
        return "AbacatePay API error: " + (error != null ? error : "empty response");
    }
}
