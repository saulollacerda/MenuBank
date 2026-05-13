package com.MenuBank.MenuBank.user;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@WithMockUser
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userResponse = UserResponse.builder()
                .id(userId)
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .phone("11999999999")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserRequest buildValidRequest() {
        return UserRequest.builder()
                .restaurantName("Restaurante Teste")
                .cnpj("12345678000195")
                .email("teste@email.com")
                .password("senha123")
                .confirmPassword("senha123")
                .phone("11999999999")
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/users
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/users")
    class CreateUser {

        @Test
        @DisplayName("deve retornar 201 com UserResponse ao criar usuário válido")
        void shouldReturn201WithUserResponse() throws Exception {
            given(userService.create(any(UserRequest.class))).willReturn(userResponse);

            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("teste@email.com"))
                    .andExpect(jsonPath("$.restaurantName").value("Restaurante Teste"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(UserRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando email já está em uso")
        void shouldReturn409WhenEmailAlreadyInUse() throws Exception {
            given(userService.create(any(UserRequest.class)))
                    .willThrow(new DuplicateUserException("email"));

            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 409 quando CNPJ já está em uso")
        void shouldReturn409WhenCnpjAlreadyInUse() throws Exception {
            given(userService.create(any(UserRequest.class)))
                    .willThrow(new DuplicateUserException("CNPJ"));

            mockMvc.perform(post("/api/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserById {

        @Test
        @DisplayName("deve retornar 200 com UserResponse quando usuário existe")
        void shouldReturn200WhenUserExists() throws Exception {
            given(userService.findById(userId)).willReturn(userResponse);

            mockMvc.perform(get("/api/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("teste@email.com"));
        }

        @Test
        @DisplayName("deve retornar 404 quando usuário não encontrado")
        void shouldReturn404WhenUserNotFound() throws Exception {
            given(userService.findById(userId)).willThrow(new UserNotFoundException(userId));

            mockMvc.perform(get("/api/users/{id}", userId))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/users/{id}")
    class UpdateUser {

        @Test
        @DisplayName("deve retornar 200 com UserResponse atualizado")
        void shouldReturn200WithUpdatedResponse() throws Exception {
            given(userService.update(eq(userId), any(UserRequest.class))).willReturn(userResponse);

            mockMvc.perform(put("/api/users/{id}", userId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()));
        }

        @Test
        @DisplayName("deve retornar 404 quando usuário não encontrado para atualização")
        void shouldReturn404WhenUserNotFoundForUpdate() throws Exception {
            given(userService.update(eq(userId), any(UserRequest.class)))
                    .willThrow(new UserNotFoundException(userId));

            mockMvc.perform(put("/api/users/{id}", userId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(put("/api/users/{id}", userId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(UserRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("deve retornar 204 ao deletar usuário existente")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(userService).delete(userId);

            mockMvc.perform(delete("/api/users/{id}", userId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 ao tentar deletar usuário inexistente")
        void shouldReturn404WhenUserNotFoundForDelete() throws Exception {
            willThrow(new UserNotFoundException(userId)).given(userService).delete(userId);

            mockMvc.perform(delete("/api/users/{id}", userId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
