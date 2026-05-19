package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeItemService")
class RecipeItemServiceTest {

    @Mock
    private RecipeItemRepository recipeItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private RecipeItemService recipeItemService;

    private UUID productId;
    private UUID ingredientId;
    private UUID recipeItemId;
    private UUID ownerId;
    private Product product;
    private Ingredient ingredient;
    private RecipeItem recipeItem;
    private RecipeItemRequest recipeItemRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();
        recipeItemId = UUID.randomUUID();

        product = Product.builder()
                .id(productId)
                .ownerId(ownerId)
                .name("X-Burguer")
                .price(new BigDecimal("25.00"))
                .status(ProductStatus.ACTIVE)
                .build();

        ingredient = Ingredient.builder()
                .id(ingredientId)
                .ownerId(ownerId)
                .name("Queijo Mussarela")
                .unit("kg")
                .costPerUnit(new BigDecimal("30.00"))
                .status(IngredientStatus.ACTIVE)
                .build();

        recipeItem = RecipeItem.builder()
                .id(recipeItemId)
                .product(product)
                .ingredient(ingredient)
                .quantity(new BigDecimal("0.100"))
                .build();

        recipeItemRequest = RecipeItemRequest.builder()
                .ingredientId(ingredientId)
                .quantity(new BigDecimal("0.100"))
                .build();
    }

    // -------------------------------------------------------------------------
    // addRecipeItem()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addRecipeItem()")
    class AddRecipeItem {

        @Test
        @DisplayName("deve adicionar item à ficha técnica e retornar RecipeItemResponse")
        void shouldAddRecipeItemAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(recipeItemRepository.save(any(RecipeItem.class))).willReturn(recipeItem);

            RecipeItemResponse result = recipeItemService.addRecipeItem(productId, recipeItemRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(recipeItemId);
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getIngredientId()).isEqualTo(ingredientId);
            assertThat(result.getIngredientName()).isEqualTo("Queijo Mussarela");
            assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("0.100"));
        }

        @Test
        @DisplayName("deve calcular totalCost como quantity * costPerUnit")
        void shouldCalculateTotalCost() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(recipeItemRepository.save(any(RecipeItem.class))).willReturn(recipeItem);

            RecipeItemResponse result = recipeItemService.addRecipeItem(productId, recipeItemRequest);

            // 0.100 * 30.00 = 3.000
            assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("3.000"));
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeItemService.addRecipeItem(productId, recipeItemRequest))
                    .isInstanceOf(ProductNotFoundException.class);

            then(recipeItemRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException quando ingrediente não existe")
        void shouldThrowWhenIngredientNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeItemService.addRecipeItem(productId, recipeItemRequest))
                    .isInstanceOf(IngredientNotFoundException.class);

            then(recipeItemRepository).should(never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // findByProductId()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findByProductId()")
    class FindByProductId {

        @Test
        @DisplayName("deve retornar lista de itens da ficha técnica do produto")
        void shouldReturnRecipeItemsForProduct() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(recipeItemRepository.findByProductIdAndProductOwnerId(productId, ownerId)).willReturn(List.of(recipeItem));

            List<RecipeItemResponse> result = recipeItemService.findByProductId(productId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIngredientName()).isEqualTo("Queijo Mussarela");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando produto não tem itens na ficha técnica")
        void shouldReturnEmptyListWhenNoRecipeItems() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(recipeItemRepository.findByProductIdAndProductOwnerId(productId, ownerId)).willReturn(List.of());

            List<RecipeItemResponse> result = recipeItemService.findByProductId(productId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> recipeItemService.findByProductId(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar item da ficha técnica e retornar RecipeItemResponse")
        void shouldUpdateAndReturnResponse() {
            RecipeItemRequest updateRequest = RecipeItemRequest.builder()
                    .ingredientId(ingredientId)
                    .quantity(new BigDecimal("0.200"))
                    .build();

            RecipeItem updatedItem = RecipeItem.builder()
                    .id(recipeItemId)
                    .product(product)
                    .ingredient(ingredient)
                    .quantity(new BigDecimal("0.200"))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(recipeItemRepository.findByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId))
                    .willReturn(Optional.of(recipeItem));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(recipeItemRepository.save(any(RecipeItem.class))).willReturn(updatedItem);

            RecipeItemResponse result = recipeItemService.update(productId, recipeItemId, updateRequest);

            assertThat(result.getQuantity()).isEqualByComparingTo(new BigDecimal("0.200"));
        }

        @Test
        @DisplayName("deve lançar RecipeItemNotFoundException quando item não existe")
        void shouldThrowWhenRecipeItemNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(recipeItemRepository.findByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeItemService.update(productId, recipeItemId, recipeItemRequest))
                    .isInstanceOf(RecipeItemNotFoundException.class);

            then(recipeItemRepository).should(never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar item da ficha técnica existente")
        void shouldDeleteExistingRecipeItem() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(recipeItemRepository.findByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId))
                    .willReturn(Optional.of(recipeItem));
            willDoNothing().given(recipeItemRepository).deleteByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId);

            assertThatNoException().isThrownBy(() -> recipeItemService.delete(productId, recipeItemId));

            then(recipeItemRepository).should().deleteByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId);
        }

        @Test
        @DisplayName("deve lançar RecipeItemNotFoundException quando item não existe")
        void shouldThrowWhenRecipeItemNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(recipeItemRepository.findByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> recipeItemService.delete(productId, recipeItemId))
                    .isInstanceOf(RecipeItemNotFoundException.class);

            then(recipeItemRepository).should(never()).deleteByIdAndProductIdAndProductOwnerId(any(), any(), any());
        }
    }
}



