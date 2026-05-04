package com.MenuBank.MenuBank.auth;

import com.MenuBank.MenuBank.user.DuplicateUserException;
import com.MenuBank.MenuBank.user.UserRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@WithMockUser
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private UUID userId;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        loginResponse = LoginResponse.builder()
                .token("mock-jwt-token")
                .userId(userId)
                .email("teste@email.com")
                .restaurantName("Restaurante Teste")
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("deve retornar 200 com token ao fazer login com credenciais válidas")
        void shouldReturn200WithTokenWhenCredentialsAreValid() throws Exception {
            given(authService.login(any(LoginRequest.class))).willReturn(loginResponse);

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoginRequest.builder()
                                            .email("teste@email.com")
                                            .password("senha123")
                                            .build())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("teste@email.com"))
                    .andExpect(jsonPath("$.restaurantName").value("Restaurante Teste"));
        }

        @Test
        @DisplayName("deve retornar 400 quando email está ausente")
        void shouldReturn400WhenEmailMissing() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoginRequest.builder()
                                            .password("senha123")
                                            .build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando senha está ausente")
        void shouldReturn400WhenPasswordMissing() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoginRequest.builder()
                                            .email("teste@email.com")
                                            .build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 401 quando credenciais são inválidas")
        void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoginRequest.builder()
                                            .email("teste@email.com")
                                            .password("senhaerrada")
                                            .build())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 403 quando usuário está inativo")
        void shouldReturn403WhenUserIsInactive() throws Exception {
            given(authService.login(any(LoginRequest.class)))
                    .willThrow(new InactiveUserException());

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    LoginRequest.builder()
                                            .email("teste@email.com")
                                            .password("senha123")
                                            .build())))
                    .andExpect(status().isForbidden());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/register
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpoint {

        private UserRequest buildValidRegisterRequest() {
            return UserRequest.builder()
                    .restaurantName("Restaurante Teste")
                    .cnpj("12345678000199")
                    .email("teste@email.com")
                    .password("senha123")
                    .phone("11999999999")
                    .build();
        }

        @Test
        @DisplayName("deve retornar 201 com token ao registrar usuário válido")
        void shouldReturn201WithTokenWhenRegistrationIsValid() throws Exception {
            given(authService.register(any(UserRequest.class))).willReturn(loginResponse);

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRegisterRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("teste@email.com"))
                    .andExpect(jsonPath("$.restaurantName").value("Restaurante Teste"));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(UserRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando email já está cadastrado")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            given(authService.register(any(UserRequest.class)))
                    .willThrow(new DuplicateUserException("email"));

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRegisterRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 409 quando CNPJ já está cadastrado")
        void shouldReturn409WhenCnpjAlreadyExists() throws Exception {
            given(authService.register(any(UserRequest.class)))
                    .willThrow(new DuplicateUserException("CNPJ"));

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRegisterRequest())))
                    .andExpect(status().isConflict());
        }
    }
}

