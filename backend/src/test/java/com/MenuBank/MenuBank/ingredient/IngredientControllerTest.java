package com.MenuBank.MenuBank.ingredient;

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

@WebMvcTest(IngredientController.class)
@WithMockUser
@DisplayName("IngredientController")
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngredientService ingredientService;

    private UUID ingredientId;
    private IngredientResponse ingredientResponse;

    @BeforeEach
    void setUp() {
        ingredientId = UUID.randomUUID();

        ingredientResponse = IngredientResponse.builder()
                .id(ingredientId)
                .name("Farinha de Trigo")
                .unit("kg")
                .costPerUnit(new BigDecimal("4.50"))
                .defaultQuantity(new BigDecimal("0.20"))
                .status(IngredientStatus.ACTIVE)
                .build();
    }

    private IngredientRequest buildValidRequest() {
        return IngredientRequest.builder()
                .name("Farinha de Trigo")
                .unit("kg")
                .costPerUnit(new BigDecimal("4.50"))
                .defaultQuantity(new BigDecimal("0.20"))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/ingredients
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/ingredients")
    class CreateIngredient {

        @Test
        @DisplayName("deve retornar 201 com IngredientResponse ao criar ingrediente válido")
        void shouldReturn201WithIngredientResponse() throws Exception {
            given(ingredientService.create(any(IngredientRequest.class))).willReturn(ingredientResponse);

            mockMvc.perform(post("/api/ingredients")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ingredientId.toString()))
                    .andExpect(jsonPath("$.name").value("Farinha de Trigo"))
                    .andExpect(jsonPath("$.unit").value("kg"))
                    .andExpect(jsonPath("$.costPerUnit").value(4.50))
                    .andExpect(jsonPath("$.defaultQuantity").value(0.20));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/ingredients")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(IngredientRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando nome já está em uso")
        void shouldReturn409WhenNameAlreadyInUse() throws Exception {
            given(ingredientService.create(any(IngredientRequest.class)))
                    .willThrow(new DuplicateIngredientException("nome"));

            mockMvc.perform(post("/api/ingredients")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/ingredients/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/ingredients/{id}")
    class GetIngredientById {

        @Test
        @DisplayName("deve retornar 200 com IngredientResponse quando ingrediente existe")
        void shouldReturn200WhenIngredientExists() throws Exception {
            given(ingredientService.findById(ingredientId)).willReturn(ingredientResponse);

            mockMvc.perform(get("/api/ingredients/{id}", ingredientId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ingredientId.toString()))
                    .andExpect(jsonPath("$.name").value("Farinha de Trigo"))
                    .andExpect(jsonPath("$.defaultQuantity").value(0.20));
        }

        @Test
        @DisplayName("deve retornar 404 quando ingrediente não encontrado")
        void shouldReturn404WhenIngredientNotFound() throws Exception {
            given(ingredientService.findById(ingredientId))
                    .willThrow(new IngredientNotFoundException(ingredientId));

            mockMvc.perform(get("/api/ingredients/{id}", ingredientId))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/ingredients
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/ingredients")
    class GetAllIngredients {

        @Test
        @DisplayName("deve retornar 200 com lista de ingredientes")
        void shouldReturn200WithIngredientList() throws Exception {
            given(ingredientService.findAll()).willReturn(List.of(ingredientResponse));

            mockMvc.perform(get("/api/ingredients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(ingredientId.toString()))
                    .andExpect(jsonPath("$[0].defaultQuantity").value(0.20));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia quando não há ingredientes")
        void shouldReturn200WithEmptyList() throws Exception {
            given(ingredientService.findAll()).willReturn(List.of());

            mockMvc.perform(get("/api/ingredients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/ingredients/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/ingredients/{id}")
    class UpdateIngredient {

        @Test
        @DisplayName("deve retornar 200 com IngredientResponse atualizado")
        void shouldReturn200WithUpdatedResponse() throws Exception {
            given(ingredientService.update(eq(ingredientId), any(IngredientRequest.class)))
                    .willReturn(ingredientResponse);

            mockMvc.perform(put("/api/ingredients/{id}", ingredientId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ingredientId.toString()))
                    .andExpect(jsonPath("$.defaultQuantity").value(0.20));
        }

        @Test
        @DisplayName("deve retornar 404 quando ingrediente não encontrado para atualização")
        void shouldReturn404WhenIngredientNotFoundForUpdate() throws Exception {
            given(ingredientService.update(eq(ingredientId), any(IngredientRequest.class)))
                    .willThrow(new IngredientNotFoundException(ingredientId));

            mockMvc.perform(put("/api/ingredients/{id}", ingredientId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(put("/api/ingredients/{id}", ingredientId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(IngredientRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/ingredients/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/ingredients/{id}")
    class DeleteIngredient {

        @Test
        @DisplayName("deve retornar 204 ao deletar ingrediente existente")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(ingredientService).delete(ingredientId);

            mockMvc.perform(delete("/api/ingredients/{id}", ingredientId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 ao tentar deletar ingrediente inexistente")
        void shouldReturn404WhenIngredientNotFoundForDelete() throws Exception {
            willThrow(new IngredientNotFoundException(ingredientId))
                    .given(ingredientService).delete(ingredientId);

            mockMvc.perform(delete("/api/ingredients/{id}", ingredientId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
