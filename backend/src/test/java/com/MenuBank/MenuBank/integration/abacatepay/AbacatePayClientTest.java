package com.MenuBank.MenuBank.integration.abacatepay;

import com.MenuBank.MenuBank.integration.abacatepay.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("AbacatePayClient")
class AbacatePayClientTest {

    private static final String BASE_URL = "https://api.abacatepay.com/v2";

    private MockRestServiceServer server;
    private AbacatePayClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AbacatePayClient(builder, BASE_URL, "test-api-key");
    }

    @Test
    @DisplayName("createProduct envia bearer token e retorna o id do produto")
    void createProduct_shouldSendAuthAndReturnProductId() {
        server.expect(requestTo(BASE_URL + "/products/create"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
              .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.externalId").value("plan-123"))
              .andExpect(jsonPath("$.name").value("Plano Básico"))
              .andExpect(jsonPath("$.price").value(5000))
              .andExpect(jsonPath("$.currency").value("BRL"))
              .andRespond(withSuccess("""
                      {"data":{"id":"prod_abc123"},"error":null}
                      """, MediaType.APPLICATION_JSON));

        String productId = client.createProduct(AbacatePayProductRequest.builder()
                .externalId("plan-123")
                .name("Plano Básico")
                .price(5000)
                .currency("BRL")
                .build());

        assertThat(productId).isEqualTo("prod_abc123");
        server.verify();
    }

    @Test
    @DisplayName("createCheckout envia itens, cliente e URLs e retorna id + url do checkout")
    void createCheckout_shouldSendPayloadAndReturnCheckout() {
        server.expect(requestTo(BASE_URL + "/checkouts/create"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
              .andExpect(jsonPath("$.items[0].id").value("prod_abc123"))
              .andExpect(jsonPath("$.items[0].quantity").value(1))
              .andExpect(jsonPath("$.customer.name").value("Restaurante X"))
              .andExpect(jsonPath("$.customer.email").value("x@email.com"))
              .andExpect(jsonPath("$.customer.taxId").value("12345678000199"))
              .andExpect(jsonPath("$.externalId").value("menubank:sub-1"))
              .andExpect(jsonPath("$.returnUrl").value("https://app.menubank.com/settings"))
              .andExpect(jsonPath("$.completionUrl").value("https://app.menubank.com/settings?paid=1"))
              .andRespond(withSuccess("""
                      {"data":{"id":"bill_xyz","url":"https://pay.abacatepay.com/bill_xyz"},"error":null}
                      """, MediaType.APPLICATION_JSON));

        AbacatePayCheckoutData checkout = client.createCheckout(AbacatePayCheckoutRequest.builder()
                .items(List.of(new AbacatePayCheckoutItem("prod_abc123", 1)))
                .customer(AbacatePayCustomer.builder()
                        .name("Restaurante X")
                        .cellphone("11999999999")
                        .email("x@email.com")
                        .taxId("12345678000199")
                        .build())
                .externalId("menubank:sub-1")
                .metadata(Map.of("merchantId", "sub-1"))
                .returnUrl("https://app.menubank.com/settings")
                .completionUrl("https://app.menubank.com/settings?paid=1")
                .build());

        assertThat(checkout.getId()).isEqualTo("bill_xyz");
        assertThat(checkout.getUrl()).isEqualTo("https://pay.abacatepay.com/bill_xyz");
        server.verify();
    }

    @Test
    @DisplayName("deve lançar AbacatePayException quando a API retorna erro")
    void shouldThrowWhenApiReturnsError() {
        server.expect(requestTo(BASE_URL + "/products/create"))
              .andRespond(withSuccess("""
                      {"data":null,"error":"Invalid API key"}
                      """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createProduct(AbacatePayProductRequest.builder()
                .externalId("plan-123")
                .name("Plano Básico")
                .price(5000)
                .currency("BRL")
                .build()))
                .isInstanceOf(AbacatePayException.class)
                .hasMessageContaining("Invalid API key");
    }
}
