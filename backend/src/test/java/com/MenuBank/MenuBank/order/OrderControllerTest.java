package com.MenuBank.MenuBank.order;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@WithMockUser
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .productName("Hambúrguer")
                .quantity(2)
                .unitPrice(new BigDecimal("30.00"))
                .build();

        orderResponse = OrderResponse.builder()
                .id(orderId)
                .dateTime(LocalDateTime.now())
                .customerId(customerId)
                .customerName("Cliente Teste")
                .status(OrderStatus.PAID)
                .totalValue(new BigDecimal("60.00"))
                .estimatedProfit(new BigDecimal("36.00"))
                .items(List.of(itemResponse))
                .build();
    }

    private OrderRequest buildValidRequest() {
        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        return OrderRequest.builder()
                .customerId(customerId)
                .items(List.of(itemRequest))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/orders
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrder {

        @Test
        @DisplayName("deve retornar 201 com OrderResponse ao criar pedido válido")
        void shouldReturn201WithOrderResponse() throws Exception {
            given(orderService.create(any(OrderRequest.class))).willReturn(orderResponse);

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(orderId.toString()))
                    .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.customerName").value("Cliente Teste"))
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.totalValue").value(60.00))
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items[0].productName").value("Hambúrguer"));
        }

        @Test
        @DisplayName("deve aceitar ingrediente extra no item do pedido")
        void shouldAcceptExtraIngredientsOnCreate() throws Exception {
            OrderRequest requestWithExtra = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(OrderItemRequest.builder()
                            .productId(productId)
                            .quantity(2)
                            .extraIngredients(List.of(
                                    OrderItemExtraIngredientRequest.builder()
                                            .ingredientId(UUID.randomUUID())
                                            .quantity(new BigDecimal("1.5"))
                                            .build()
                            ))
                            .build()))
                    .build();

            given(orderService.create(any(OrderRequest.class))).willReturn(orderResponse);

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithExtra)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(OrderRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando lista de itens está vazia")
        void shouldReturn400WhenItemsListIsEmpty() throws Exception {
            OrderRequest emptyItems = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of())
                    .build();

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyItems)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando ingrediente extra possui campos inválidos")
        void shouldReturn400WhenExtraIngredientIsInvalid() throws Exception {
            // ingredientId ausente
            OrderRequest invalid = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(OrderItemRequest.builder()
                            .productId(productId)
                            .quantity(1)
                            .extraIngredients(List.of(
                                    OrderItemExtraIngredientRequest.builder()
                                            .quantity(new BigDecimal("1"))
                                            .build()
                            ))
                            .build()))
                    .build();

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            then(orderService).should(never()).create(any(OrderRequest.class));
        }

        @Test
        @DisplayName("deve retornar 404 quando cliente não encontrado")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            given(orderService.create(any(OrderRequest.class)))
                    .willThrow(new OrderNotFoundException("Cliente com ID " + customerId + " não encontrado"));

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não encontrado")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(orderService.create(any(OrderRequest.class)))
                    .willThrow(new OrderNotFoundException("Produto com ID " + productId + " não encontrado"));

            mockMvc.perform(post("/api/orders")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/orders/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderById {

        @Test
        @DisplayName("deve retornar 200 com OrderResponse quando pedido existe")
        void shouldReturn200WhenOrderExists() throws Exception {
            given(orderService.findById(orderId)).willReturn(orderResponse);

            mockMvc.perform(get("/api/orders/{id}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderId.toString()))
                    .andExpect(jsonPath("$.status").value("PAID"))
                    .andExpect(jsonPath("$.totalValue").value(60.00));
        }

        @Test
        @DisplayName("deve retornar 404 quando pedido não encontrado")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            given(orderService.findById(orderId)).willThrow(new OrderNotFoundException(orderId));

            mockMvc.perform(get("/api/orders/{id}", orderId))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/orders
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/orders")
    class GetAllOrders {

        @Test
        @DisplayName("deve retornar 200 com lista de pedidos")
        void shouldReturn200WithOrderList() throws Exception {
            given(orderService.findAll()).willReturn(List.of(orderResponse));

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                    .andExpect(jsonPath("$[0].status").value("PAID"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando não há pedidos")
        void shouldReturn200WithEmptyList() throws Exception {
            given(orderService.findAll()).willReturn(List.of());

            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/orders/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/orders/{id}")
    class UpdateOrder {

        @Test
        @DisplayName("deve retornar 200 com OrderResponse atualizado")
        void shouldReturn200WithUpdatedResponse() throws Exception {
            given(orderService.update(eq(orderId), any(OrderRequest.class))).willReturn(orderResponse);

            mockMvc.perform(put("/api/orders/{id}", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderId.toString()));
        }

        @Test
        @DisplayName("deve retornar 404 quando pedido não encontrado para atualização")
        void shouldReturn404WhenOrderNotFoundForUpdate() throws Exception {
            given(orderService.update(eq(orderId), any(OrderRequest.class)))
                    .willThrow(new OrderNotFoundException(orderId));

            mockMvc.perform(put("/api/orders/{id}", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(put("/api/orders/{id}", orderId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(OrderRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/orders/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/orders/{id}")
    class DeleteOrder {

        @Test
        @DisplayName("deve retornar 204 ao deletar pedido existente")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(orderService).delete(orderId);

            mockMvc.perform(delete("/api/orders/{id}", orderId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 ao tentar deletar pedido inexistente")
        void shouldReturn404WhenOrderNotFoundForDelete() throws Exception {
            willThrow(new OrderNotFoundException(orderId)).given(orderService).delete(orderId);

            mockMvc.perform(delete("/api/orders/{id}", orderId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}

