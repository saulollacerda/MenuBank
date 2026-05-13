package com.MenuBank.MenuBank.user;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userRequest = UserRequest.builder()
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("senha123")
                .confirmPassword("senha123")
                .phone("11999999999")
                .build();

        user = User.builder()
                .id(userId)
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("$2a$10$encodedpassword")
                .phone("11999999999")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar usuário com dados válidos e retornar UserResponse")
        void shouldCreateUserAndReturnUserResponse() {
            given(userRepository.existsByEmail(userRequest.getEmail())).willReturn(false);
            given(userRepository.existsByCnpj(userRequest.getCnpj())).willReturn(false);
            given(passwordEncoder.encode(userRequest.getPassword())).willReturn("$2a$10$encodedpassword");
            given(userRepository.save(any(User.class))).willReturn(user);

            UserResponse result = userService.create(userRequest);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(userRequest.getEmail());
            assertThat(result.getRestaurantName()).isEqualTo(userRequest.getRestaurantName());
            assertThat(result.getCnpj()).isEqualTo(userRequest.getCnpj());
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("deve criar usuário com status ACTIVE por padrão")
        void shouldCreateUserWithActiveStatusByDefault() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByCnpj(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$10$encodedpassword");
            given(userRepository.save(any(User.class))).willReturn(user);

            UserResponse result = userService.create(userRequest);

            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve encriptar a senha antes de salvar")
        void shouldEncryptPasswordBeforeSaving() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByCnpj(anyString())).willReturn(false);
            given(passwordEncoder.encode(userRequest.getPassword())).willReturn("$2a$10$encodedpassword");
            given(userRepository.save(any(User.class))).willReturn(user);

            userService.create(userRequest);

            then(passwordEncoder).should().encode(userRequest.getPassword());
            then(userRepository).should().save(argThat(u ->
                    !u.getPassword().equals(userRequest.getPassword())
            ));
        }

        @Test
        @DisplayName("deve lançar DuplicateUserException quando email já está cadastrado")
        void shouldThrowWhenEmailAlreadyExists() {
            given(userRepository.existsByEmail(userRequest.getEmail())).willReturn(true);

            assertThatThrownBy(() -> userService.create(userRequest))
                    .isInstanceOf(DuplicateUserException.class)
                    .hasMessageContaining("email");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("deve lançar DuplicateUserException quando CNPJ já está cadastrado")
        void shouldThrowWhenCnpjAlreadyExists() {
            given(userRepository.existsByEmail(userRequest.getEmail())).willReturn(false);
            given(userRepository.existsByCnpj(userRequest.getCnpj())).willReturn(true);

            assertThatThrownBy(() -> userService.create(userRequest))
                    .isInstanceOf(DuplicateUserException.class)
                    .hasMessageContaining("CNPJ");

            then(userRepository).should(never()).save(any(User.class));
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar UserResponse quando usuário existe")
        void shouldReturnUserResponseWhenExists() {
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            UserResponse result = userService.findById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo(user.getEmail());
            assertThat(result.getRestaurantName()).isEqualTo(user.getRestaurantName());
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException quando usuário não existe")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista de todos os usuários")
        void shouldReturnListOfAllUsers() {
            given(userRepository.findAll()).willReturn(List.of(user));

            List<UserResponse> result = userService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há usuários")
        void shouldReturnEmptyList() {
            given(userRepository.findAll()).willReturn(List.of());

            List<UserResponse> result = userService.findAll();

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar usuário existente e retornar UserResponse atualizado")
        void shouldUpdateAndReturnUpdatedUserResponse() {
            UserRequest updateRequest = UserRequest.builder()
                    .restaurantName("Restaurante Atualizado")
                    .cnpj("12345678000195")
                    .email("teste@email.com")
                    .password("novaSenha123")
                    .confirmPassword("novaSenha123")
                    .phone("11988888888")
                    .build();

            User updatedUser = User.builder()
                    .id(userId)
                    .restaurantName("Restaurante Atualizado")
                    .cnpj("12345678000195")
                    .email("teste@email.com")
                    .password("$2a$10$newencoded")
                    .phone("11988888888")
                    .status(UserStatus.ACTIVE)
                    .createdAt(user.getCreatedAt())
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.encode(updateRequest.getPassword())).willReturn("$2a$10$newencoded");
            given(userRepository.save(any(User.class))).willReturn(updatedUser);

            UserResponse result = userService.update(userId, updateRequest);

            assertThat(result.getRestaurantName()).isEqualTo("Restaurante Atualizado");
            assertThat(result.getPhone()).isEqualTo("11988888888");
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException ao atualizar usuário inexistente")
        void shouldThrowWhenUserNotFoundForUpdate() {
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(userId, userRequest))
                    .isInstanceOf(UserNotFoundException.class);

            then(userRepository).should(never()).save(any(User.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar usuário existente sem lançar exceção")
        void shouldDeleteExistingUser() {
            given(userRepository.existsById(userId)).willReturn(true);
            willDoNothing().given(userRepository).deleteById(userId);

            assertThatNoException().isThrownBy(() -> userService.delete(userId));

            then(userRepository).should().deleteById(userId);
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException ao deletar usuário inexistente")
        void shouldThrowWhenUserNotFoundForDelete() {
            given(userRepository.existsById(userId)).willReturn(false);

            assertThatThrownBy(() -> userService.delete(userId))
                    .isInstanceOf(UserNotFoundException.class);

            then(userRepository).should(never()).deleteById(any());
        }
    }
}

