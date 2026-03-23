package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
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

@WebMvcTest(RecipeItemController.class)
@WithMockUser
@DisplayName("RecipeItemController")
class RecipeItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeItemService recipeItemService;

    private UUID productId;
    private UUID recipeItemId;
    private UUID ingredientId;
    private RecipeItemResponse recipeItemResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        recipeItemId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();

        recipeItemResponse = RecipeItemResponse.builder()
                .id(recipeItemId)
                .productId(productId)
                .ingredientId(ingredientId)
                .ingredientName("Queijo Mussarela")
                .ingredientUnit("kg")
                .quantity(new BigDecimal("0.100"))
                .costPerUnit(new BigDecimal("30.00"))
                .totalCost(new BigDecimal("3.000"))
                .build();
    }

    private RecipeItemRequest buildValidRequest() {
        return RecipeItemRequest.builder()
                .ingredientId(ingredientId)
                .quantity(new BigDecimal("0.100"))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/products/{productId}/recipe-items
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/products/{productId}/recipe-items")
    class AddRecipeItem {

        @Test
        @DisplayName("deve retornar 201 com RecipeItemResponse ao adicionar item válido")
        void shouldReturn201WithRecipeItemResponse() throws Exception {
            given(recipeItemService.addRecipeItem(eq(productId), any(RecipeItemRequest.class)))
                    .willReturn(recipeItemResponse);

            mockMvc.perform(post("/api/products/{productId}/recipe-items", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(recipeItemId.toString()))
                    .andExpect(jsonPath("$.ingredientName").value("Queijo Mussarela"))
                    .andExpect(jsonPath("$.quantity").value(0.100))
                    .andExpect(jsonPath("$.totalCost").value(3.000));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/recipe-items", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(RecipeItemRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(recipeItemService.addRecipeItem(eq(productId), any(RecipeItemRequest.class)))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(post("/api/products/{productId}/recipe-items", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 404 quando ingrediente não existe")
        void shouldReturn404WhenIngredientNotFound() throws Exception {
            given(recipeItemService.addRecipeItem(eq(productId), any(RecipeItemRequest.class)))
                    .willThrow(new IngredientNotFoundException(ingredientId));

            mockMvc.perform(post("/api/products/{productId}/recipe-items", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/products/{productId}/recipe-items
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/products/{productId}/recipe-items")
    class GetRecipeItems {

        @Test
        @DisplayName("deve retornar 200 com lista de itens da ficha técnica")
        void shouldReturn200WithRecipeItemList() throws Exception {
            given(recipeItemService.findByProductId(productId))
                    .willReturn(List.of(recipeItemResponse));

            mockMvc.perform(get("/api/products/{productId}/recipe-items", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].ingredientName").value("Queijo Mussarela"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia")
        void shouldReturn200WithEmptyList() throws Exception {
            given(recipeItemService.findByProductId(productId)).willReturn(List.of());

            mockMvc.perform(get("/api/products/{productId}/recipe-items", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(recipeItemService.findByProductId(productId))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(get("/api/products/{productId}/recipe-items", productId))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/products/{productId}/recipe-items/{recipeItemId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/products/{productId}/recipe-items/{recipeItemId}")
    class UpdateRecipeItem {

        @Test
        @DisplayName("deve retornar 200 com RecipeItemResponse atualizado")
        void shouldReturn200WithUpdatedResponse() throws Exception {
            given(recipeItemService.update(eq(productId), eq(recipeItemId), any(RecipeItemRequest.class)))
                    .willReturn(recipeItemResponse);

            mockMvc.perform(put("/api/products/{productId}/recipe-items/{recipeItemId}", productId, recipeItemId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(recipeItemId.toString()));
        }

        @Test
        @DisplayName("deve retornar 404 quando item não encontrado")
        void shouldReturn404WhenRecipeItemNotFound() throws Exception {
            given(recipeItemService.update(eq(productId), eq(recipeItemId), any(RecipeItemRequest.class)))
                    .willThrow(new RecipeItemNotFoundException(recipeItemId));

            mockMvc.perform(put("/api/products/{productId}/recipe-items/{recipeItemId}", productId, recipeItemId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/products/{productId}/recipe-items/{recipeItemId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/products/{productId}/recipe-items/{recipeItemId}")
    class DeleteRecipeItem {

        @Test
        @DisplayName("deve retornar 204 ao deletar item existente")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(recipeItemService).delete(productId, recipeItemId);

            mockMvc.perform(delete("/api/products/{productId}/recipe-items/{recipeItemId}", productId, recipeItemId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando item não encontrado para deletar")
        void shouldReturn404WhenRecipeItemNotFoundForDelete() throws Exception {
            willThrow(new RecipeItemNotFoundException(recipeItemId))
                    .given(recipeItemService).delete(productId, recipeItemId);

            mockMvc.perform(delete("/api/products/{productId}/recipe-items/{recipeItemId}", productId, recipeItemId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}

