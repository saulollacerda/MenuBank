package com.MenuBank.MenuBank.integration.ifood;

import com.MenuBank.MenuBank.auth.AuthHelper;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodUserCodeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;



import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IfoodAuthController.class)
@WithMockUser
@DisplayName("IfoodAuthController")
class IfoodAuthControllerTest {

    @Autowired private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private IfoodTokenService tokenService;
    @MockitoBean private AuthHelper authHelper;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        given(authHelper.getMerchantId(any())).willReturn(merchantId);
    }

    @Test
    @DisplayName("POST /api/integrations/ifood/auth/start retorna userCode e expiresIn")
    void start_shouldReturnUserCode() throws Exception {
        IfoodUserCodeResponse response = new IfoodUserCodeResponse();
        response.setUserCode("HJLX-LPSQ");
        response.setVerificationUrl("https://portal.ifood.com.br/apps/code");
        response.setExpiresIn(600);
        given(tokenService.startAuthorization(merchantId)).willReturn(response);

        mockMvc.perform(post("/api/integrations/ifood/auth/start").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCode").value("HJLX-LPSQ"))
                .andExpect(jsonPath("$.verificationUrl").value("https://portal.ifood.com.br/apps/code"))
                .andExpect(jsonPath("$.expiresIn").value(600));
    }

    @Test
    @DisplayName("POST /api/integrations/ifood/auth/connect retorna 200 após conectar")
    void connect_shouldReturn200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("authorizationCode", "auth-code-123"));

        mockMvc.perform(post("/api/integrations/ifood/auth/connect").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(tokenService).connect(any(), eq("auth-code-123"));
    }

    @Test
    @DisplayName("DELETE /api/integrations/ifood/auth/revoke retorna 204")
    void revoke_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/integrations/ifood/auth/revoke").with(csrf()))
                .andExpect(status().isNoContent());

        verify(tokenService).revoke(any());
    }

    @Test
    @DisplayName("POST /api/integrations/ifood/auth/connect retorna 400 sem authorizationCode")
    void connect_shouldReturn400WhenAuthorizationCodeMissing() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of());

        mockMvc.perform(post("/api/integrations/ifood/auth/connect").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
