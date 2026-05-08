package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.user.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User activeUser;
    private User inactiveUser;
    private LoginRequest loginRequest;
    private UserRequest registerRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        loginRequest = LoginRequest.builder()
                .email("teste@email.com")
                .password("senha123")
                .build();

        registerRequest = UserRequest.builder()
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("senha123")
                .confirmPassword("senha123")
                .phone("11999999999")
                .build();

        activeUser = User.builder()
                .id(userId)
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("$2a$10$encodedpassword")
                .phone("11999999999")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        inactiveUser = User.builder()
                .id(userId)
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("$2a$10$encodedpassword")
                .phone("11999999999")
                .status(UserStatus.INACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void mockJwtEncoder() {
        Jwt jwt = Jwt.withTokenValue("mock-jwt-token")
                .header("alg", "RS256")
                .claim("sub", userId.toString())
                .claim("email", "teste@email.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
        given(jwtEncoder.encode(any(JwtEncoderParameters.class))).willReturn(jwt);
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("deve autenticar usuário com credenciais válidas e retornar token JWT")
        void shouldAuthenticateAndReturnJwtToken() {
            given(userRepository.findByEmail(loginRequest.getEmail()))
                    .willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(loginRequest.getPassword(), activeUser.getPassword()))
                    .willReturn(true);
            mockJwtEncoder();

            LoginResponse result = authService.login(loginRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("mock-jwt-token");
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo("teste@email.com");
            assertThat(result.getRestaurantName()).isEqualTo("Restaurante Teste");
        }

        @Test
        @DisplayName("deve gerar token JWT via JwtEncoder")
        void shouldGenerateTokenViaJwtEncoder() {
            given(userRepository.findByEmail(loginRequest.getEmail()))
                    .willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(loginRequest.getPassword(), activeUser.getPassword()))
                    .willReturn(true);
            mockJwtEncoder();

            authService.login(loginRequest);

            then(jwtEncoder).should().encode(any(JwtEncoderParameters.class));
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando email não encontrado")
        void shouldThrowWhenEmailNotFound() {
            given(userRepository.findByEmail(loginRequest.getEmail()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            then(passwordEncoder).should(never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando senha incorreta")
        void shouldThrowWhenPasswordIsWrong() {
            given(userRepository.findByEmail(loginRequest.getEmail()))
                    .willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(loginRequest.getPassword(), activeUser.getPassword()))
                    .willReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class);

            then(jwtEncoder).should(never()).encode(any());
        }

        @Test
        @DisplayName("deve lançar InactiveUserException quando usuário está inativo")
        void shouldThrowWhenUserIsInactive() {
            given(userRepository.findByEmail(loginRequest.getEmail()))
                    .willReturn(Optional.of(inactiveUser));
            given(passwordEncoder.matches(loginRequest.getPassword(), inactiveUser.getPassword()))
                    .willReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(InactiveUserException.class);

            then(jwtEncoder).should(never()).encode(any());
        }
    }

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("deve registrar usuário e retornar token JWT (auto-login)")
        void shouldRegisterAndReturnJwtToken() {
            UserResponse userResponse = UserResponse.builder()
                    .id(userId)
                    .restaurantName("Restaurante Teste")
                    .cnpj("12345678000195")
                    .email("teste@email.com")
                    .phone("11999999999")
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(userService.create(registerRequest)).willReturn(userResponse);
            mockJwtEncoder();

            LoginResponse result = authService.register(registerRequest);

            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("mock-jwt-token");
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo("teste@email.com");
            assertThat(result.getRestaurantName()).isEqualTo("Restaurante Teste");
        }

        @Test
        @DisplayName("deve delegar criação do usuário ao UserService")
        void shouldDelegateToUserService() {
            UserResponse userResponse = UserResponse.builder()
                    .id(userId)
                    .restaurantName("Restaurante Teste")
                    .cnpj("12345678000195")
                    .email("teste@email.com")
                    .phone("11999999999")
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(userService.create(registerRequest)).willReturn(userResponse);
            mockJwtEncoder();

            authService.register(registerRequest);

            then(userService).should().create(registerRequest);
        }

        @Test
        @DisplayName("deve propagar DuplicateUserException do UserService")
        void shouldPropagateDuplicateUserException() {
            given(userService.create(registerRequest))
                    .willThrow(new DuplicateUserException("email"));

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(DuplicateUserException.class);
        }
    }
}

