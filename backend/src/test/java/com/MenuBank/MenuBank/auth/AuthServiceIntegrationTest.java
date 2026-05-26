package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.integration.IntegrationTestBase;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRequest;
import com.MenuBank.MenuBank.merchant.MerchantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthService — integração com Postgres")
class AuthServiceIntegrationTest extends IntegrationTestBase {

    @Autowired private AuthService authService;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final AtomicLong CNPJ_SEQ = new AtomicLong(30_000_000_000_000L);

    @Test
    @DisplayName("register deve criar merchant e retornar JWT válido")
    void register_shouldCreateMerchantAndReturnToken() {
        MerchantRequest request = newRequest("Registrado");

        LoginResponse response = authService.register(request);

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getMerchantId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());

        Merchant persisted = merchantRepository.findById(response.getMerchantId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        // Senha gravada como hash, não texto puro
        assertThat(passwordEncoder.matches("password123", persisted.getPassword())).isTrue();
    }

    @Test
    @DisplayName("login deve retornar JWT com credenciais corretas")
    void login_shouldReturnTokenForValidCredentials() {
        MerchantRequest reg = newRequest("Login Teste");
        authService.register(reg);

        LoginResponse loginResponse = authService.login(LoginRequest.builder()
                .email(reg.getEmail()).password("password123").build());

        assertThat(loginResponse.getToken()).isNotBlank();
        assertThat(loginResponse.getEmail()).isEqualTo(reg.getEmail());
    }

    @Test
    @DisplayName("login deve falhar com email inexistente")
    void login_shouldFailWhenEmailNotFound() {
        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .email("inexistente@example.com").password("any").build()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login deve falhar com senha errada")
    void login_shouldFailWithWrongPassword() {
        MerchantRequest reg = newRequest("Senha Errada");
        authService.register(reg);

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .email(reg.getEmail()).password("wrong").build()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login deve falhar quando o merchant está INACTIVE")
    void login_shouldFailWhenMerchantIsInactive() {
        MerchantRequest reg = newRequest("Inativo");
        LoginResponse registered = authService.register(reg);
        Merchant merchant = merchantRepository.findById(registered.getMerchantId()).orElseThrow();
        merchant.setStatus(MerchantStatus.INACTIVE);
        merchantRepository.save(merchant);

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .email(reg.getEmail()).password("password123").build()))
                .isInstanceOf(InactiveMerchantException.class);
    }

    private MerchantRequest newRequest(String name) {
        long cnpjN = CNPJ_SEQ.incrementAndGet();
        return MerchantRequest.builder()
                .merchantName(name)
                .cnpj(String.valueOf(cnpjN))
                .email(UUID.randomUUID() + "@example.com")
                .password("password123")
                .confirmPassword("password123")
                .phone("11999990000")
                .build();
    }
}
