package com.MenuBank.MenuBank.validation;

import com.MenuBank.MenuBank.user.UserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordMatchValidator")
class PasswordMatchValidatorTest {

    private final PasswordMatchValidator validator = new PasswordMatchValidator();

    @Test
    @DisplayName("deve aceitar quando senha e confirmação são iguais")
    void shouldAcceptWhenPasswordsMatch() {
        UserRequest request = UserRequest.builder()
                .password("senha123")
                .confirmPassword("senha123")
                .build();

        assertThat(validator.isValid(request, null)).isTrue();
    }

    @Test
    @DisplayName("deve rejeitar quando senha e confirmação são diferentes")
    void shouldRejectWhenPasswordsDoNotMatch() {
        UserRequest request = UserRequest.builder()
                .password("senha123")
                .confirmPassword("outraSenha")
                .build();

        assertThat(validator.isValid(request, null)).isFalse();
    }

    @Test
    @DisplayName("deve ignorar validação quando senha está ausente")
    void shouldIgnoreWhenPasswordIsMissing() {
        UserRequest request = UserRequest.builder()
                .confirmPassword("senha123")
                .build();

        assertThat(validator.isValid(request, null)).isTrue();
    }
}

