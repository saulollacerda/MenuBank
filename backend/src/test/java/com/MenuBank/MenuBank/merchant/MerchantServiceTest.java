package com.MenuBank.MenuBank.merchant;

import com.MenuBank.MenuBank.common.ForbiddenException;
import com.MenuBank.MenuBank.common.MerchantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantService")
class MerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MerchantContext merchantContext;

    @InjectMocks
    private MerchantService merchantService;

    private UUID merchantId;
    private Merchant merchant;
    private MerchantRequest merchantRequest;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();

        merchantRequest = MerchantRequest.builder()
                .merchantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("senha123")
                .confirmPassword("senha123")
                .phone("11999999999")
                .build();

        merchant = Merchant.builder()
                .id(merchantId)
                .merchantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("$2a$10$encodedpassword")
                .phone("11999999999")
                .status(MerchantStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar usuário com dados válidos e retornar MerchantResponse")
        void shouldCreateUserAndReturnMerchantResponse() {
            given(merchantRepository.existsByEmail(merchantRequest.getEmail())).willReturn(false);
            given(merchantRepository.existsByCnpj(merchantRequest.getCnpj())).willReturn(false);
            given(passwordEncoder.encode(merchantRequest.getPassword())).willReturn("$2a$10$encodedpassword");
            given(merchantRepository.save(any(Merchant.class))).willReturn(merchant);

            MerchantResponse result = merchantService.create(merchantRequest);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(merchantRequest.getEmail());
            assertThat(result.getMerchantName()).isEqualTo(merchantRequest.getMerchantName());
            assertThat(result.getCnpj()).isEqualTo(merchantRequest.getCnpj());
            then(merchantRepository).should().save(any(Merchant.class));
        }

        @Test
        @DisplayName("deve criar usuário com status ACTIVE por padrão")
        void shouldCreateUserWithActiveStatusByDefault() {
            given(merchantRepository.existsByEmail(anyString())).willReturn(false);
            given(merchantRepository.existsByCnpj(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedpassword");
            given(merchantRepository.save(any(Merchant.class))).willReturn(merchant);

            MerchantResponse result = merchantService.create(merchantRequest);

            assertThat(result.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve encriptar a senha antes de salvar")
        void shouldEncryptPasswordBeforeSaving() {
            given(merchantRepository.existsByEmail(anyString())).willReturn(false);
            given(merchantRepository.existsByCnpj(anyString())).willReturn(false);
            given(passwordEncoder.encode(merchantRequest.getPassword())).willReturn("$2a$10$encodedpassword");
            given(merchantRepository.save(any(Merchant.class))).willReturn(merchant);

            merchantService.create(merchantRequest);

            then(passwordEncoder).should().encode(merchantRequest.getPassword());
            then(merchantRepository).should().save(argThat(u ->
                    !u.getPassword().equals(merchantRequest.getPassword())
            ));
        }

        @Test
        @DisplayName("deve lançar DuplicateMerchantException quando email já está cadastrado")
        void shouldThrowWhenEmailAlreadyExists() {
            given(merchantRepository.existsByEmail(merchantRequest.getEmail())).willReturn(true);

            assertThatThrownBy(() -> merchantService.create(merchantRequest))
                    .isInstanceOf(DuplicateMerchantException.class)
                    .hasMessageContaining("email");

            then(merchantRepository).should(never()).save(any(Merchant.class));
        }

        @Test
        @DisplayName("deve lançar DuplicateMerchantException quando CNPJ já está cadastrado")
        void shouldThrowWhenCnpjAlreadyExists() {
            given(merchantRepository.existsByEmail(merchantRequest.getEmail())).willReturn(false);
            given(merchantRepository.existsByCnpj(merchantRequest.getCnpj())).willReturn(true);

            assertThatThrownBy(() -> merchantService.create(merchantRequest))
                    .isInstanceOf(DuplicateMerchantException.class)
                    .hasMessageContaining("CNPJ");

            then(merchantRepository).should(never()).save(any(Merchant.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar MerchantResponse quando o usuário é o próprio")
        void shouldReturnMerchantResponseWhenOwner() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));

            MerchantResponse result = merchantService.findById(merchantId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(merchantId);
        }

        @Test
        @DisplayName("deve lançar ForbiddenException ao tentar ler outro usuário")
        void shouldThrowForbiddenWhenAccessingAnotherUser() {
            given(merchantContext.getMerchantId()).willReturn(UUID.randomUUID());

            assertThatThrownBy(() -> merchantService.findById(merchantId))
                    .isInstanceOf(ForbiddenException.class);

            then(merchantRepository).should(never()).findById(any());
        }

        @Test
        @DisplayName("deve lançar MerchantNotFoundException quando usuário não existe")
        void shouldThrowWhenUserNotFound() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.findById(merchantId))
                    .isInstanceOf(MerchantNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar o próprio usuário e retornar MerchantResponse atualizado")
        void shouldUpdateAndReturnUpdatedMerchantResponse() {
            MerchantRequest updateRequest = MerchantRequest.builder()
                    .merchantName("Restaurante Atualizado")
                    .cnpj("12345678000195")
                    .email("teste@email.com")
                    .password("novaSenha123")
                    .confirmPassword("novaSenha123")
                    .phone("11988888888")
                    .build();

            Merchant updatedUser = Merchant.builder()
                    .id(merchantId)
                    .merchantName("Restaurante Atualizado")
                    .cnpj("12345678000195")
                    .email("teste@email.com")
                    .password("$2a$10$newencoded")
                    .phone("11988888888")
                    .status(MerchantStatus.ACTIVE)
                    .createdAt(merchant.getCreatedAt())
                    .build();

            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(passwordEncoder.encode(updateRequest.getPassword())).willReturn("$2a$10$newencoded");
            given(merchantRepository.save(any(Merchant.class))).willReturn(updatedUser);

            MerchantResponse result = merchantService.update(merchantId, updateRequest);

            assertThat(result.getMerchantName()).isEqualTo("Restaurante Atualizado");
            assertThat(result.getPhone()).isEqualTo("11988888888");
        }

        @Test
        @DisplayName("deve lançar ForbiddenException ao tentar atualizar outro usuário")
        void shouldThrowForbiddenWhenUpdatingAnotherUser() {
            given(merchantContext.getMerchantId()).willReturn(UUID.randomUUID());

            assertThatThrownBy(() -> merchantService.update(merchantId, merchantRequest))
                    .isInstanceOf(ForbiddenException.class);

            then(merchantRepository).should(never()).save(any(Merchant.class));
        }

        @Test
        @DisplayName("deve lançar MerchantNotFoundException ao atualizar usuário inexistente")
        void shouldThrowWhenUserNotFoundForUpdate() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.update(merchantId, merchantRequest))
                    .isInstanceOf(MerchantNotFoundException.class);

            then(merchantRepository).should(never()).save(any(Merchant.class));
        }
    }

    @Nested
    @DisplayName("updateAnotaAIKey()")
    class UpdateAnotaAIKey {

        @Test
        @DisplayName("deve salvar a chave do Anota.AI no usuário autenticado")
        void shouldSaveKeyOnCurrentUser() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(merchantRepository.save(any(Merchant.class))).willAnswer(inv -> inv.getArgument(0));

            MerchantResponse result = merchantService.updateAnotaAIKey(new AnotaAIKeyRequest("my-key"));

            assertThat(result.getAnotaAiApiKey()).isEqualTo("my-key");
            assertThat(merchant.getAnotaAiApiKey()).isEqualTo("my-key");
            then(passwordEncoder).should(never()).encode(anyString());
        }

        @Test
        @DisplayName("deve aceitar chave nula (remover a chave)")
        void shouldAcceptNullKey() {
            merchant.setAnotaAiApiKey("old-key");
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(merchantRepository.save(any(Merchant.class))).willAnswer(inv -> inv.getArgument(0));

            MerchantResponse result = merchantService.updateAnotaAIKey(new AnotaAIKeyRequest(null));

            assertThat(result.getAnotaAiApiKey()).isNull();
            assertThat(merchant.getAnotaAiApiKey()).isNull();
        }

        @Test
        @DisplayName("deve lançar MerchantNotFoundException se usuário autenticado não existir")
        void shouldThrowIfUserNotFound() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.findById(merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> merchantService.updateAnotaAIKey(new AnotaAIKeyRequest("k")))
                    .isInstanceOf(MerchantNotFoundException.class);

            then(merchantRepository).should(never()).save(any(Merchant.class));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar o próprio usuário sem lançar exceção")
        void shouldDeleteOwnUser() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.existsById(merchantId)).willReturn(true);
            willDoNothing().given(merchantRepository).deleteById(merchantId);

            assertThatNoException().isThrownBy(() -> merchantService.delete(merchantId));

            then(merchantRepository).should().deleteById(merchantId);
        }

        @Test
        @DisplayName("deve lançar ForbiddenException ao tentar deletar outro usuário")
        void shouldThrowForbiddenWhenDeletingAnotherUser() {
            given(merchantContext.getMerchantId()).willReturn(UUID.randomUUID());

            assertThatThrownBy(() -> merchantService.delete(merchantId))
                    .isInstanceOf(ForbiddenException.class);

            then(merchantRepository).should(never()).deleteById(any());
        }

        @Test
        @DisplayName("deve lançar MerchantNotFoundException ao deletar usuário inexistente")
        void shouldThrowWhenUserNotFoundForDelete() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(merchantRepository.existsById(merchantId)).willReturn(false);

            assertThatThrownBy(() -> merchantService.delete(merchantId))
                    .isInstanceOf(MerchantNotFoundException.class);

            then(merchantRepository).should(never()).deleteById(any());
        }
    }
}
