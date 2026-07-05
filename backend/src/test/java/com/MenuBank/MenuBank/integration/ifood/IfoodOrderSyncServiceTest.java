package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.integration.ifood.dto.IfoodEventResponse;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodOrderDetailResponse;
import com.MenuBank.MenuBank.integration.ifood.services.IfoodOrderImportService;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("IfoodOrderSyncService")
class IfoodOrderSyncServiceTest {

    @Mock private IfoodOrderClient orderClient;
    @Mock private IfoodTokenService tokenService;
    @Mock private MerchantRepository merchantRepository;
    @Mock private IfoodOrderImportService importService;

    @InjectMocks
    private IfoodOrderSyncService syncService;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        merchant = Merchant.builder().id(UUID.randomUUID()).ifoodMerchantId("ifood-m1").build();
        lenient().when(merchantRepository.findAllByIfoodMerchantIdIsNotNull()).thenReturn(List.of(merchant));
        lenient().when(tokenService.getAccessToken()).thenReturn("token-1");
    }

    private IfoodEventResponse event(String id, String fullCode, String orderId) {
        IfoodEventResponse event = new IfoodEventResponse();
        event.setId(id);
        event.setCode(fullCode.substring(0, Math.min(3, fullCode.length())));
        event.setFullCode(fullCode);
        event.setOrderId(orderId);
        event.setMerchantId("ifood-m1");
        return event;
    }

    private IfoodOrderDetailResponse detail(String orderId) {
        IfoodOrderDetailResponse detail = new IfoodOrderDetailResponse();
        detail.setId(orderId);
        return detail;
    }

    private static HttpClientErrorException unauthorized() {
        return HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }

    private static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("não chama a API quando nenhum merchant tem iFood autorizado")
    void shouldNotCallApiWhenNoAuthorizedMerchants() {
        given(merchantRepository.findAllByIfoodMerchantIdIsNotNull()).willReturn(List.of());

        syncService.syncOrders();

        then(tokenService).should(never()).getAccessToken();
        then(orderClient).should(never()).pollEvents(anyString(), anyList());
    }

    @Nested
    @DisplayName("evento CONFIRMED")
    class ConfirmedEvent {

        @Test
        @DisplayName("busca o detalhe e importa com status PENDING")
        void shouldImportConfirmedOrderAsPending() {
            IfoodOrderDetailResponse detail = detail("ord-1");
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CONFIRMED", "ord-1")));
            given(orderClient.getOrderDetail("token-1", "ord-1")).willReturn(detail);

            syncService.syncOrders();

            then(importService).should().importOrder(detail, OrderStatus.PENDING);
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }

        @Test
        @DisplayName("aceita a variante ORDER_CONFIRMED (case-insensitive)")
        void shouldAcceptOrderPrefixedVariant() {
            IfoodOrderDetailResponse detail = detail("ord-1");
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "order_confirmed", "ord-1")));
            given(orderClient.getOrderDetail("token-1", "ord-1")).willReturn(detail);

            syncService.syncOrders();

            then(importService).should().importOrder(detail, OrderStatus.PENDING);
        }

        @Test
        @DisplayName("404 no detalhe é pulado sem importar — o CONCLUDED importa depois (safety net)")
        void shouldSkipConfirmedWhenDetailNotFound() {
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CONFIRMED", "ord-1")));
            given(orderClient.getOrderDetail("token-1", "ord-1")).willThrow(notFound());

            syncService.syncOrders();

            then(importService).should(never()).importOrder(any(), any());
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }
    }

    @Nested
    @DisplayName("evento CONCLUDED")
    class ConcludedEvent {

        @Test
        @DisplayName("pedido existente é atualizado para PAID sem buscar o detalhe")
        void shouldConcludeExistingOrderWithoutFetchingDetail() {
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CONCLUDED", "ord-1")));
            given(importService.concludeOrder("ord-1", "ifood-m1")).willReturn(true);

            syncService.syncOrders();

            then(orderClient).should(never()).getOrderDetail(anyString(), anyString());
            then(importService).should(never()).importOrder(any(), any());
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }

        @Test
        @DisplayName("pedido inexistente é importado completo com status PAID (fallback)")
        void shouldImportUnknownConcludedOrderAsPaid() {
            IfoodOrderDetailResponse detail = detail("ord-1");
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CONCLUDED", "ord-1")));
            given(importService.concludeOrder("ord-1", "ifood-m1")).willReturn(false);
            given(orderClient.getOrderDetail("token-1", "ord-1")).willReturn(detail);

            syncService.syncOrders();

            then(importService).should().importOrder(detail, OrderStatus.PAID);
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }

        @Test
        @DisplayName("aceita a variante ORDER_CONCLUDED (case-insensitive)")
        void shouldAcceptOrderPrefixedVariant() {
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "ORDER_CONCLUDED", "ord-1")));
            given(importService.concludeOrder("ord-1", "ifood-m1")).willReturn(true);

            syncService.syncOrders();

            then(importService).should().concludeOrder("ord-1", "ifood-m1");
        }
    }

    @Nested
    @DisplayName("evento CANCELLED")
    class CancelledEvent {

        @Test
        @DisplayName("pedido existente é cancelado sem buscar o detalhe")
        void shouldCancelExistingOrderWithoutFetchingDetail() {
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CANCELLED", "ord-1")));
            given(importService.cancelOrder("ord-1", "ifood-m1")).willReturn(true);

            syncService.syncOrders();

            then(orderClient).should(never()).getOrderDetail(anyString(), anyString());
            then(importService).should(never()).importOrder(any(), any());
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }

        @Test
        @DisplayName("pedido inexistente é importado com status CANCELLED")
        void shouldImportUnknownCancelledOrder() {
            IfoodOrderDetailResponse detail = detail("ord-1");
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CANCELLED", "ord-1")));
            given(importService.cancelOrder("ord-1", "ifood-m1")).willReturn(false);
            given(orderClient.getOrderDetail("token-1", "ord-1")).willReturn(detail);

            syncService.syncOrders();

            then(importService).should().importOrder(detail, OrderStatus.CANCELLED);
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }

        @Test
        @DisplayName("404 no detalhe de pedido inexistente é logado e pulado, mas o evento é reconhecido")
        void shouldSkipUnknownCancelledOrderWhenDetailNotFound() {
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "CANCELLED", "ord-1")));
            given(importService.cancelOrder("ord-1", "ifood-m1")).willReturn(false);
            given(orderClient.getOrderDetail("token-1", "ord-1")).willThrow(notFound());

            syncService.syncOrders();

            then(importService).should(never()).importOrder(any(), any());
            then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1"));
        }

        @Test
        @DisplayName("aceita a variante ORDER_CANCELLED (case-insensitive)")
        void shouldAcceptOrderPrefixedVariant() {
            given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                    .willReturn(List.of(event("evt-1", "Order_Cancelled", "ord-1")));
            given(importService.cancelOrder("ord-1", "ifood-m1")).willReturn(true);

            syncService.syncOrders();

            then(importService).should().cancelOrder("ord-1", "ifood-m1");
        }
    }

    @Test
    @DisplayName("evento fora de CONFIRMED/CANCELLED/CONCLUDED é apenas reconhecido, sem ação")
    void shouldOnlyAcknowledgeIgnoredEvents() {
        given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                .willReturn(List.of(
                        event("evt-2", "PLACED", "ord-2"),
                        event("evt-3", "DISPATCHED", "ord-3")));

        syncService.syncOrders();

        then(orderClient).should(never()).getOrderDetail(anyString(), anyString());
        then(importService).should(never()).importOrder(any(), any());
        then(importService).should(never()).concludeOrder(anyString(), anyString());
        then(importService).should(never()).cancelOrder(anyString(), anyString());
        then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-2", "evt-3"));
    }

    @Test
    @DisplayName("falha em um pedido não impede o processamento dos demais nem o acknowledgment")
    void shouldContinueProcessingWhenOneOrderFails() {
        IfoodOrderDetailResponse detail3 = detail("ord-3");
        given(orderClient.pollEvents("token-1", List.of("ifood-m1")))
                .willReturn(List.of(
                        event("evt-1", "CONCLUDED", "ord-1"),
                        event("evt-3", "CONCLUDED", "ord-3")));
        given(importService.concludeOrder(anyString(), anyString())).willReturn(false);
        given(orderClient.getOrderDetail("token-1", "ord-1"))
                .willThrow(new RuntimeException("boom"));
        given(orderClient.getOrderDetail("token-1", "ord-3")).willReturn(detail3);

        syncService.syncOrders();

        then(importService).should().importOrder(detail3, OrderStatus.PAID);
        then(orderClient).should().acknowledgeEvents("token-1", List.of("evt-1", "evt-3"));
    }

    @Test
    @DisplayName("401 no polling força refresh do token e repete a chamada uma única vez")
    void shouldRetryPollOnceOn401() {
        given(orderClient.pollEvents("token-1", List.of("ifood-m1"))).willThrow(unauthorized());
        given(tokenService.handleUnauthorized()).willReturn("token-2");
        given(orderClient.pollEvents("token-2", List.of("ifood-m1"))).willReturn(List.of());

        syncService.syncOrders();

        then(tokenService).should().handleUnauthorized();
        then(orderClient).should().pollEvents("token-2", List.of("ifood-m1"));
    }

    @Test
    @DisplayName("não reconhece nada quando o polling não retorna eventos")
    void shouldNotAcknowledgeWhenNoEvents() {
        given(orderClient.pollEvents("token-1", List.of("ifood-m1"))).willReturn(List.of());

        syncService.syncOrders();

        then(orderClient).should(never()).acknowledgeEvents(anyString(), anyList());
    }
}
