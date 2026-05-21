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
@DisplayName("ProductIngredientService")
class ProductIngredientServiceTest {

    @Mock
    private ProductIngredientRepository productIngredientRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private ProductIngredientService productIngredientService;

    private UUID productId;
    private UUID ingredientId;
    private UUID productIngredientId;
    private UUID ownerId;
    private Product product;
    private Ingredient ingredient;
    private ProductIngredient productIngredient;
    private ProductIngredientRequest productIngredientRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();
        productIngredientId = UUID.randomUUID();

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

        productIngredient = ProductIngredient.builder()
                .id(productIngredientId)
                .product(product)
                .ingredient(ingredient)
                .grammage(new BigDecimal("0.100"))
                .build();

        productIngredientRequest = ProductIngredientRequest.builder()
                .ingredientId(ingredientId)
                .grammage(new BigDecimal("0.100"))
                .build();
    }

    // -------------------------------------------------------------------------
    // addProductIngredient()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addProductIngredient()")
    class AddProductIngredient {

        @Test
        @DisplayName("deve adicionar item à ficha técnica e retornar ProductIngredientResponse")
        void shouldAddProductIngredientAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(productIngredientRepository.save(any(ProductIngredient.class))).willReturn(productIngredient);

            ProductIngredientResponse result = productIngredientService.addProductIngredient(productId, productIngredientRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productIngredientId);
            assertThat(result.getProductId()).isEqualTo(productId);
            assertThat(result.getIngredientId()).isEqualTo(ingredientId);
            assertThat(result.getIngredientName()).isEqualTo("Queijo Mussarela");
            assertThat(result.getGrammage()).isEqualByComparingTo(new BigDecimal("0.100"));
        }

        @Test
        @DisplayName("deve calcular totalCost como quantity * costPerUnit")
        void shouldCalculateTotalCost() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(productIngredientRepository.save(any(ProductIngredient.class))).willReturn(productIngredient);

            ProductIngredientResponse result = productIngredientService.addProductIngredient(productId, productIngredientRequest);

            // 0.100 * 30.00 = 3.000
            assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("3.000"));
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService.addProductIngredient(productId, productIngredientRequest))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productIngredientRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException quando ingrediente não existe")
        void shouldThrowWhenIngredientNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService.addProductIngredient(productId, productIngredientRequest))
                    .isInstanceOf(IngredientNotFoundException.class);

            then(productIngredientRepository).should(never()).save(any());
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
        void shouldReturnProductIngredientsForProduct() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId)).willReturn(List.of(productIngredient));

            List<ProductIngredientResponse> result = productIngredientService.findByProductId(productId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIngredientName()).isEqualTo("Queijo Mussarela");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando produto não tem itens na ficha técnica")
        void shouldReturnEmptyListWhenNoProductIngredients() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId)).willReturn(List.of());

            List<ProductIngredientResponse> result = productIngredientService.findByProductId(productId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> productIngredientService.findByProductId(productId))
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
        @DisplayName("deve atualizar item da ficha técnica e retornar ProductIngredientResponse")
        void shouldUpdateAndReturnResponse() {
            ProductIngredientRequest updateRequest = ProductIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    .grammage(new BigDecimal("0.200"))
                    .build();

            ProductIngredient updatedItem = ProductIngredient.builder()
                    .id(productIngredientId)
                    .product(product)
                    .ingredient(ingredient)
                    .grammage(new BigDecimal("0.200"))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productIngredientRepository.findByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId))
                    .willReturn(Optional.of(productIngredient));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(productIngredientRepository.save(any(ProductIngredient.class))).willReturn(updatedItem);

            ProductIngredientResponse result = productIngredientService.update(productId, productIngredientId, updateRequest);

            assertThat(result.getGrammage()).isEqualByComparingTo(new BigDecimal("0.200"));
        }

        @Test
        @DisplayName("deve lançar ProductIngredientNotFoundException quando item não existe")
        void shouldThrowWhenProductIngredientNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productIngredientRepository.findByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService.update(productId, productIngredientId, productIngredientRequest))
                    .isInstanceOf(ProductIngredientNotFoundException.class);

            then(productIngredientRepository).should(never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // isOptional + updateGrammageByIngredientId
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isOptional flag e updateGrammageByIngredientId")
    class OptionalFlagAndGrammageUpdate {

        @Test
        @DisplayName("addProductIngredient deve persistir o flag isOptional quando informado")
        void setIsOptionalFlag_shouldPersistFlag() {
            ProductIngredientRequest request = ProductIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    .grammage(new BigDecimal("30"))
                    .isOptional(true)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(productIngredientRepository.save(any(ProductIngredient.class)))
                    .willAnswer(inv -> { ProductIngredient pi = inv.getArgument(0); pi.setId(UUID.randomUUID()); return pi; });

            ProductIngredientResponse result = productIngredientService.addProductIngredient(productId, request);

            assertThat(result.isOptional()).isTrue();

            ArgumentCaptor<ProductIngredient> captor = ArgumentCaptor.forClass(ProductIngredient.class);
            then(productIngredientRepository).should().save(captor.capture());
            assertThat(captor.getValue().isOptional()).isTrue();
        }

        @Test
        @DisplayName("addProductIngredient sem isOptional informado deve usar false como default")
        void setIsOptionalFlag_shouldDefaultToFalseWhenNull() {
            ProductIngredientRequest request = ProductIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    .grammage(new BigDecimal("30"))
                    .build();  // isOptional = null

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(productIngredientRepository.save(any(ProductIngredient.class)))
                    .willAnswer(inv -> { ProductIngredient pi = inv.getArgument(0); pi.setId(UUID.randomUUID()); return pi; });

            ProductIngredientResponse result = productIngredientService.addProductIngredient(productId, request);

            assertThat(result.isOptional()).isFalse();
        }

        @Test
        @DisplayName("updateGrammageByIngredientId deve atualizar grammage de ProductIngredient já associado")
        void updateGrammageByIngredientId_shouldUpdateExistingProductIngredient() {
            ProductIngredient existing = ProductIngredient.builder()
                    .id(productIngredientId)
                    .product(product)
                    .ingredient(ingredient)
                    .grammage(new BigDecimal("100"))
                    .isOptional(false)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(productIngredientRepository.findByProductIdAndIngredientIdAndProductOwnerId(productId, ingredientId, ownerId))
                    .willReturn(Optional.of(existing));
            given(productIngredientRepository.save(any(ProductIngredient.class))).willReturn(existing);

            ProductIngredientResponse result = productIngredientService
                    .updateGrammageByIngredientId(productId, ingredientId, new BigDecimal("250"));

            assertThat(result.getGrammage()).isEqualByComparingTo("250");
            assertThat(existing.getGrammage()).isEqualByComparingTo("250");
        }

        @Test
        @DisplayName("updateGrammageByIngredientId deve lançar quando ingrediente não está associado ao produto")
        void updateGrammageByIngredientId_shouldThrowWhenNotAssociated() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(productIngredientRepository.findByProductIdAndIngredientIdAndProductOwnerId(productId, ingredientId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService
                    .updateGrammageByIngredientId(productId, ingredientId, new BigDecimal("250")))
                    .isInstanceOf(ProductIngredientNotFoundException.class);

            then(productIngredientRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("updateGrammageByIngredientId deve lançar ProductNotFoundException quando produto não existe")
        void updateGrammageByIngredientId_shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> productIngredientService
                    .updateGrammageByIngredientId(productId, ingredientId, new BigDecimal("250")))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productIngredientRepository).should(never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // addProductIngredientsBatch()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addProductIngredientsBatch()")
    class AddProductIngredientsBatch {

        @Test
        @DisplayName("deve salvar todos os items quando todos os ingredientes existem")
        void shouldSaveAllItemsAtomically() {
            UUID ingredient2Id = UUID.randomUUID();
            Ingredient ingredient2 = Ingredient.builder()
                    .id(ingredient2Id).ownerId(ownerId).name("Bacon")
                    .unit("un").costPerUnit(new BigDecimal("2.00"))
                    .status(IngredientStatus.ACTIVE).build();

            ProductIngredientRequest req1 = ProductIngredientRequest.builder()
                    .ingredientId(ingredientId).grammage(new BigDecimal("0.100")).build();
            ProductIngredientRequest req2 = ProductIngredientRequest.builder()
                    .ingredientId(ingredient2Id).grammage(new BigDecimal("1")).build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(ingredientRepository.findByIdAndOwnerId(ingredient2Id, ownerId)).willReturn(Optional.of(ingredient2));
            given(productIngredientRepository.save(any(ProductIngredient.class)))
                    .willAnswer(inv -> { ProductIngredient r = inv.getArgument(0); r.setId(UUID.randomUUID()); return r; });

            List<ProductIngredientResponse> result = productIngredientService.addProductIngredientsBatch(productId, List.of(req1, req2));

            assertThat(result).hasSize(2);
            then(productIngredientRepository).should(times(2)).save(any(ProductIngredient.class));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException sem chamar save quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService.addProductIngredientsBatch(
                    productId, List.of(productIngredientRequest)))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productIngredientRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException quando algum ingrediente não existe (rollback transacional)")
        void shouldRollbackWhenAnyIngredientNotFound() {
            UUID missingIngredientId = UUID.randomUUID();
            ProductIngredientRequest req1 = ProductIngredientRequest.builder()
                    .ingredientId(ingredientId).grammage(new BigDecimal("0.100")).build();
            ProductIngredientRequest req2 = ProductIngredientRequest.builder()
                    .ingredientId(missingIngredientId).grammage(new BigDecimal("1")).build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(ingredientRepository.findByIdAndOwnerId(missingIngredientId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService.addProductIngredientsBatch(productId, List.of(req1, req2)))
                    .isInstanceOf(com.MenuBank.MenuBank.ingredient.IngredientNotFoundException.class);

            // Annotation @Transactional faz rollback — aqui validamos que o save não foi efetivado
            // verificando que o método lança antes mesmo de chegar a salvar o segundo, OU que se chegou
            // a salvar o primeiro será rolled back pelo Spring. Mocks não simulam isso, mas validamos
            // que a exceção é propagada (que dispara o rollback do Spring).
        }
    }

    // -------------------------------------------------------------------------
    // deleteAllByProductId()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteAllByProductId()")
    class DeleteAllByProductId {

        @Test
        @DisplayName("deve deletar todos os ProductIngredients do produto")
        void shouldDeleteAllProductIngredientsForProduct() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            given(productIngredientRepository.deleteAllByProductIdAndProductOwnerId(productId, ownerId)).willReturn(5L);

            long deleted = productIngredientService.deleteAllByProductId(productId);

            assertThat(deleted).isEqualTo(5L);
            then(productIngredientRepository).should().deleteAllByProductIdAndProductOwnerId(productId, ownerId);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> productIngredientService.deleteAllByProductId(productId))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productIngredientRepository).should(never()).deleteAllByProductIdAndProductOwnerId(any(), any());
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
        void shouldDeleteExistingProductIngredient() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productIngredientRepository.findByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId))
                    .willReturn(Optional.of(productIngredient));
            willDoNothing().given(productIngredientRepository).deleteByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId);

            assertThatNoException().isThrownBy(() -> productIngredientService.delete(productId, productIngredientId));

            then(productIngredientRepository).should().deleteByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId);
        }

        @Test
        @DisplayName("deve lançar ProductIngredientNotFoundException quando item não existe")
        void shouldThrowWhenProductIngredientNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productIngredientRepository.findByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> productIngredientService.delete(productId, productIngredientId))
                    .isInstanceOf(ProductIngredientNotFoundException.class);

            then(productIngredientRepository).should(never()).deleteByIdAndProductIdAndProductOwnerId(any(), any(), any());
        }
    }
}



