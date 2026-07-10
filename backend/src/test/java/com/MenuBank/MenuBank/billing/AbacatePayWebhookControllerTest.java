package com.MenuBank.MenuBank.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AbacatePayWebhookController.class)
@WithMockUser
@DisplayName("AbacatePayWebhookController")
class AbacatePayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AbacatePayBillingService billingService;

    private static final String VALID_SECRET = "test-webhook-secret";

    @Nested
    @DisplayName("POST /api/webhooks/abacatepay")
    class HandleWebhook {

        @Test
        @DisplayName("deve processar billing.paid e retornar 200 quando o secret é válido")
        void shouldProcessBillingPaidWithValidSecret() throws Exception {
            mockMvc.perform(post("/api/webhooks/abacatepay")
                            .param("webhookSecret", VALID_SECRET)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "id": "evt_1",
                                      "event": "billing.paid",
                                      "data": {
                                        "billing": {
                                          "id": "bill_xyz",
                                          "externalId": "menubank:m-1:p-1",
                                          "amount": 5000
                                        }
                                      }
                                    }
                                    """))
                    .andExpect(status().isOk());

            then(billingService).should().handleBillingPaid("bill_xyz", "menubank:m-1:p-1", 5000L);
        }

        @Test
        @DisplayName("deve retornar 401 e não processar quando o secret é inválido")
        void shouldReturn401WithInvalidSecret() throws Exception {
            mockMvc.perform(post("/api/webhooks/abacatepay")
                            .param("webhookSecret", "wrong-secret")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"id": "evt_1", "event": "billing.paid", "data": {}}
                                    """))
                    .andExpect(status().isUnauthorized());

            then(billingService).should(never()).handleBillingPaid(anyString(), anyString(), anyLong());
        }

        @Test
        @DisplayName("deve retornar 200 sem processar eventos que não são billing.paid")
        void shouldIgnoreOtherEvents() throws Exception {
            mockMvc.perform(post("/api/webhooks/abacatepay")
                            .param("webhookSecret", VALID_SECRET)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"id": "evt_2", "event": "payout.done", "data": {"id": "payout_1"}}
                                    """))
                    .andExpect(status().isOk());

            then(billingService).should(never()).handleBillingPaid(any(), any(), any());
        }
    }
}
