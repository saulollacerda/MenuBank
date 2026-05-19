package com.MenuBank.MenuBank.payment;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentMethodController.class)
@WithMockUser
@DisplayName("PaymentMethodController")
class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentMethodService paymentMethodService;

    private UUID paymentMethodId;
    private PaymentMethodResponse paymentMethodResponse;

    @BeforeEach
    void setUp() {
        paymentMethodId = UUID.randomUUID();

        paymentMethodResponse = PaymentMethodResponse.builder()
                .id(paymentMethodId)
                .name("Crédito")
                .feeRate(new BigDecimal("2.5000"))
                .build();
    }

    private PaymentMethodRequest buildValidRequest() {
        return PaymentMethodRequest.builder()
                .name("Crédito")
                .feeRate(new BigDecimal("2.5000"))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/payment-methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/payment-methods")
    class Create {

        @Test
        @DisplayName("deve retornar 201 com PaymentMethodResponse ao criar com dados válidos")
        void shouldReturn201WithResponse() throws Exception {
            given(paymentMethodService.create(any(PaymentMethodRequest.class))).willReturn(paymentMethodResponse);

            mockMvc.perform(post("/api/payment-methods")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(paymentMethodId.toString()))
                    .andExpect(jsonPath("$.name").value("Crédito"))
                    .andExpect(jsonPath("$.feeRate").value(2.5));
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está ausente")
        void shouldReturn400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/api/payment-methods")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    PaymentMethodRequest.builder().feeRate(new BigDecimal("2.5")).build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando taxa está ausente")
        void shouldReturn400WhenFeeRateMissing() throws Exception {
            mockMvc.perform(post("/api/payment-methods")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    PaymentMethodRequest.builder().name("Crédito").build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando nome já está em uso")
        void shouldReturn409WhenNameAlreadyInUse() throws Exception {
            given(paymentMethodService.create(any(PaymentMethodRequest.class)))
                    .willThrow(new DuplicatePaymentMethodException("nome"));

            mockMvc.perform(post("/api/payment-methods")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/payment-methods/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/payment-methods/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 com PaymentMethodResponse quando existe")
        void shouldReturn200WhenExists() throws Exception {
            given(paymentMethodService.findById(paymentMethodId)).willReturn(paymentMethodResponse);

            mockMvc.perform(get("/api/payment-methods/{id}", paymentMethodId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(paymentMethodId.toString()))
                    .andExpect(jsonPath("$.name").value("Crédito"));
        }

        @Test
        @DisplayName("deve retornar 404 quando não encontrada")
        void shouldReturn404WhenNotFound() throws Exception {
            given(paymentMethodService.findById(paymentMethodId))
                    .willThrow(new PaymentMethodNotFoundException(paymentMethodId));

            mockMvc.perform(get("/api/payment-methods/{id}", paymentMethodId))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/payment-methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/payment-methods")
    class GetAll {

        @Test
        @DisplayName("deve retornar 200 com página de formas de pagamento")
        void shouldReturn200WithPage() throws Exception {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(paymentMethodService.findAll(eq(""), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(
                            List.of(paymentMethodResponse), pageable, 1));

            mockMvc.perform(get("/api/payment-methods"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(paymentMethodId.toString()))
                    .andExpect(jsonPath("$.content[0].name").value("Crédito"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("deve repassar parâmetro search ao service")
        void shouldPassSearchParamToService() throws Exception {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(paymentMethodService.findAll(eq("cred"), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(
                            List.of(paymentMethodResponse), pageable, 1));

            mockMvc.perform(get("/api/payment-methods").param("search", "cred"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Crédito"));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/payment-methods/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/payment-methods/{id}")
    class Update {

        @Test
        @DisplayName("deve retornar 200 com PaymentMethodResponse atualizado")
        void shouldReturn200WithUpdatedResponse() throws Exception {
            given(paymentMethodService.update(eq(paymentMethodId), any(PaymentMethodRequest.class)))
                    .willReturn(paymentMethodResponse);

            mockMvc.perform(put("/api/payment-methods/{id}", paymentMethodId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(paymentMethodId.toString()));
        }

        @Test
        @DisplayName("deve retornar 404 quando não encontrada para atualização")
        void shouldReturn404WhenNotFound() throws Exception {
            given(paymentMethodService.update(eq(paymentMethodId), any(PaymentMethodRequest.class)))
                    .willThrow(new PaymentMethodNotFoundException(paymentMethodId));

            mockMvc.perform(put("/api/payment-methods/{id}", paymentMethodId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando dados inválidos na atualização")
        void shouldReturn400WhenInvalidDataForUpdate() throws Exception {
            mockMvc.perform(put("/api/payment-methods/{id}", paymentMethodId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(PaymentMethodRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/payment-methods/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/payment-methods/{id}")
    class Delete {

        @Test
        @DisplayName("deve retornar 204 ao deletar forma de pagamento existente")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(paymentMethodService).delete(paymentMethodId);

            mockMvc.perform(delete("/api/payment-methods/{id}", paymentMethodId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 ao tentar deletar forma inexistente")
        void shouldReturn404WhenNotFound() throws Exception {
            willThrow(new PaymentMethodNotFoundException(paymentMethodId))
                    .given(paymentMethodService).delete(paymentMethodId);

            mockMvc.perform(delete("/api/payment-methods/{id}", paymentMethodId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
