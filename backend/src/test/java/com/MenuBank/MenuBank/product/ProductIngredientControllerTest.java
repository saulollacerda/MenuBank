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

@WebMvcTest(ProductIngredientController.class)
@WithMockUser
@DisplayName("ProductIngredientController")
class ProductIngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductIngredientService productIngredientService;

    private UUID productId;
    private UUID productIngredientId;
    private UUID ingredientId;
    private ProductIngredientResponse productIngredientResponse;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        productIngredientId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();

        productIngredientResponse = ProductIngredientResponse.builder()
                .id(productIngredientId)
                .productId(productId)
                .ingredientId(ingredientId)
                .ingredientName("Queijo Mussarela")
                .ingredientUnit("kg")
                .grammage(new BigDecimal("0.100"))
                .costPerUnit(new BigDecimal("30.00"))
                .totalCost(new BigDecimal("3.000"))
                .build();
    }

    private ProductIngredientRequest buildValidRequest() {
        return ProductIngredientRequest.builder()
                .ingredientId(ingredientId)
                .grammage(new BigDecimal("0.100"))
                .build();
    }

    // -------------------------------------------------------------------------
    // POST /api/products/{productId}/ingredients
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/products/{productId}/ingredients")
    class AddProductIngredient {

        @Test
        @DisplayName("deve retornar 201 com ProductIngredientResponse ao adicionar item válido")
        void shouldReturn201WithProductIngredientResponse() throws Exception {
            given(productIngredientService.addProductIngredient(eq(productId), any(ProductIngredientRequest.class)))
                    .willReturn(productIngredientResponse);

            mockMvc.perform(post("/api/products/{productId}/ingredients", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(productIngredientId.toString()))
                    .andExpect(jsonPath("$.ingredientName").value("Queijo Mussarela"))
                    .andExpect(jsonPath("$.grammage").value(0.100))
                    .andExpect(jsonPath("$.totalCost").value(3.000));
        }

        @Test
        @DisplayName("deve retornar 400 quando campos obrigatórios estão ausentes")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/ingredients", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ProductIngredientRequest.builder().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(productIngredientService.addProductIngredient(eq(productId), any(ProductIngredientRequest.class)))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(post("/api/products/{productId}/ingredients", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 404 quando ingrediente não existe")
        void shouldReturn404WhenIngredientNotFound() throws Exception {
            given(productIngredientService.addProductIngredient(eq(productId), any(ProductIngredientRequest.class)))
                    .willThrow(new IngredientNotFoundException(ingredientId));

            mockMvc.perform(post("/api/products/{productId}/ingredients", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/products/{productId}/ingredients
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/products/{productId}/ingredients")
    class GetProductIngredients {

        @Test
        @DisplayName("deve retornar 200 com lista de itens da ficha técnica")
        void shouldReturn200WithProductIngredientList() throws Exception {
            given(productIngredientService.findByProductId(productId))
                    .willReturn(List.of(productIngredientResponse));

            mockMvc.perform(get("/api/products/{productId}/ingredients", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].ingredientName").value("Queijo Mussarela"));
        }

        @Test
        @DisplayName("deve retornar 200 com lista vazia")
        void shouldReturn200WithEmptyList() throws Exception {
            given(productIngredientService.findByProductId(productId)).willReturn(List.of());

            mockMvc.perform(get("/api/products/{productId}/ingredients", productId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(productIngredientService.findByProductId(productId))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(get("/api/products/{productId}/ingredients", productId))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/products/{productId}/ingredients/{productIngredientId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/products/{productId}/ingredients/{productIngredientId}")
    class UpdateProductIngredient {

        @Test
        @DisplayName("deve retornar 200 com ProductIngredientResponse atualizado")
        void shouldReturn200WithUpdatedResponse() throws Exception {
            given(productIngredientService.update(eq(productId), eq(productIngredientId), any(ProductIngredientRequest.class)))
                    .willReturn(productIngredientResponse);

            mockMvc.perform(put("/api/products/{productId}/ingredients/{productIngredientId}", productId, productIngredientId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productIngredientId.toString()));
        }

        @Test
        @DisplayName("deve retornar 404 quando item não encontrado")
        void shouldReturn404WhenProductIngredientNotFound() throws Exception {
            given(productIngredientService.update(eq(productId), eq(productIngredientId), any(ProductIngredientRequest.class)))
                    .willThrow(new ProductIngredientNotFoundException(productIngredientId));

            mockMvc.perform(put("/api/products/{productId}/ingredients/{productIngredientId}", productId, productIngredientId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/products/{productId}/ingredients/{productIngredientId}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/products/{productId}/ingredients/batch")
    class AddProductIngredientsBatch {

        @Test
        @DisplayName("deve retornar 201 com a lista de ProductIngredientResponse criados")
        void shouldReturn201WithCreatedList() throws Exception {
            given(productIngredientService.addProductIngredientsBatch(eq(productId), any()))
                    .willReturn(List.of(productIngredientResponse));

            mockMvc.perform(post("/api/products/{productId}/ingredients/batch", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(buildValidRequest()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(productIngredientId.toString()));
        }

        @Test
        @DisplayName("deve retornar 400 quando lista vazia")
        void shouldReturn400WhenEmptyList() throws Exception {
            mockMvc.perform(post("/api/products/{productId}/ingredients/batch", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(productIngredientService.addProductIngredientsBatch(eq(productId), any()))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(post("/api/products/{productId}/ingredients/batch", productId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(buildValidRequest()))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{productId}/ingredients")
    class DeleteAllProductIngredients {

        @Test
        @DisplayName("deve retornar 200 com a contagem de itens deletados")
        void shouldReturn200WithDeletedCount() throws Exception {
            given(productIngredientService.deleteAllByProductId(productId)).willReturn(3L);

            mockMvc.perform(delete("/api/products/{productId}/ingredients", productId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deleted").value(3));
        }

        @Test
        @DisplayName("deve retornar 404 quando produto não existe")
        void shouldReturn404WhenProductNotFound() throws Exception {
            given(productIngredientService.deleteAllByProductId(productId))
                    .willThrow(new ProductNotFoundException(productId));

            mockMvc.perform(delete("/api/products/{productId}/ingredients", productId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{productId}/ingredients/{productIngredientId}")
    class DeleteProductIngredient {

        @Test
        @DisplayName("deve retornar 204 ao deletar item existente")
        void shouldReturn204WhenDeleted() throws Exception {
            willDoNothing().given(productIngredientService).delete(productId, productIngredientId);

            mockMvc.perform(delete("/api/products/{productId}/ingredients/{productIngredientId}", productId, productIngredientId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 404 quando item não encontrado para deletar")
        void shouldReturn404WhenProductIngredientNotFoundForDelete() throws Exception {
            willThrow(new ProductIngredientNotFoundException(productIngredientId))
                    .given(productIngredientService).delete(productId, productIngredientId);

            mockMvc.perform(delete("/api/products/{productId}/ingredients/{productIngredientId}", productId, productIngredientId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}

