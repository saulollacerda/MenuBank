package com.MenuBank.MenuBank.integration.anotaai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AnotaAIClient {

    private final RestClient ordersClient;
    private final RestClient menuClient;

    public AnotaAIClient(
            @Value("${anotaai.orders-base-url:https://api-parceiros.anota.ai}") String ordersBaseUrl,
            @Value("${anotaai.menu-base-url:https://api-menu.anota.ai}") String menuBaseUrl) {
        this.ordersClient = RestClient.builder().baseUrl(ordersBaseUrl).build();
        this.menuClient = RestClient.builder().baseUrl(menuBaseUrl).build();
    }

    public AnotaAIOrderListResponse getOrderList(String apiKey) {
        return ordersClient.get()
                .uri("/partnerauth/ping/list")
                .header(HttpHeaders.AUTHORIZATION, apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(AnotaAIOrderListResponse.class);
    }

    public AnotaAIOrderDetailResponse getOrderDetail(String apiKey, String orderId) {
        return ordersClient.get()
                .uri("/partnerauth/ping/get/{orderId}", orderId)
                .header(HttpHeaders.AUTHORIZATION, apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(AnotaAIOrderDetailResponse.class);
    }

    public AnotaAICatalogResponse getCatalog(String apiKey) {
        return menuClient.get()
                .uri("/partnerauth/v2/nm-category/rest/simple-item/export/v2")
                .header(HttpHeaders.AUTHORIZATION, apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(AnotaAICatalogResponse.class);
    }
}
