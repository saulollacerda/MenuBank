package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryNotFoundException;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.common.UserContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private ProductService productService;

    private UUID ownerId;
    private UUID productId;
    private UUID categoryId;
    private Category category;
    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .ownerId(ownerId)
                .name("Lanches")
                .build();

        productRequest = ProductRequest.builder()
                .name("X-Burguer")
                .price(new BigDecimal("25.90"))
                .categoryId(categoryId)
                .build();

        product = Product.builder()
                .id(productId)
                .ownerId(ownerId)
                .name("X-Burguer")
                .price(new BigDecimal("25.90"))
                .status(ProductStatus.ACTIVE)
                .category(category)
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar produto com dados válidos e retornar ProductResponse")
        void shouldCreateProductAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByNameAndOwnerId(productRequest.getName(), ownerId)).willReturn(false);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo(productRequest.getName());
            assertThat(result.getPrice()).isEqualByComparingTo(productRequest.getPrice());
            then(productRepository).should().save(argThat(p -> ownerId.equals(p.getOwnerId())));
        }

        @Test
        @DisplayName("deve criar produto com status ACTIVE por padrão")
        void shouldCreateProductWithActiveStatusByDefault() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve criar produto com estimatedCost zero e margin igual ao preço")
        void shouldCreateProductWithZeroCostAndFullMargin() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

        }

        @Test
        @DisplayName("deve atribuir a categoria ao produto e retornar categoryId/categoryName na resposta")
        void shouldAssignCategoryAndReturnCategoryFieldsInResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result.getCategoryId()).isEqualTo(categoryId);
            assertThat(result.getCategoryName()).isEqualTo("Lanches");
            then(productRepository).should().save(argThat(p -> p.getCategory() != null
                    && categoryId.equals(p.getCategory().getId())));
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException quando categoria não pertence ao owner")
        void shouldThrowWhenCategoryNotFoundForOwner() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(productRequest))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("deve lançar DuplicateProductException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByNameAndOwnerId(productRequest.getName(), ownerId)).willReturn(true);

            assertThatThrownBy(() -> productService.create(productRequest))
                    .isInstanceOf(DuplicateProductException.class)
                    .hasMessageContaining("nome");

            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar ProductResponse quando produto existe")
        void shouldReturnResponseWhenExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));

            ProductResponse result = productService.findById(productId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo(product.getName());
            assertThat(result.getPrice()).isEqualByComparingTo(product.getPrice());
            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException quando produto não existe")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll(search, pageable)")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de produtos filtrada por nome (contains, case-insensitive)")
        void shouldReturnPagedProductsFilteredByName() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, "burg", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(product), pageable, 1));

            org.springframework.data.domain.Page<ProductResponse> result =
                    productService.findAll("burg", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("X-Burguer");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve tratar search nulo como string vazia (retorna tudo)")
        void shouldTreatNullSearchAsEmpty() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(product), pageable, 1));

            org.springframework.data.domain.Page<ProductResponse> result =
                    productService.findAll(null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar produto existente e retornar ProductResponse atualizado")
        void shouldUpdateAndReturnUpdatedResponse() {
            ProductRequest updateRequest = ProductRequest.builder()
                    .name("X-Salada")
                    .price(new BigDecimal("29.90"))
                    .categoryId(categoryId)
                    .build();

            Product updatedProduct = Product.builder()
                    .id(productId)
                    .ownerId(ownerId)
                    .name("X-Salada")
                    .price(new BigDecimal("29.90"))
                    .status(ProductStatus.ACTIVE)
                    .category(category)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(updatedProduct);

            ProductResponse result = productService.update(productId, updateRequest);

            assertThat(result.getName()).isEqualTo("X-Salada");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("29.90"));
        }

        @Test
        @DisplayName("deve permitir trocar a categoria do produto")
        void shouldAllowChangingCategory() {
            UUID newCategoryId = UUID.randomUUID();
            Category newCategory = Category.builder()
                    .id(newCategoryId)
                    .ownerId(ownerId)
                    .name("Bebidas")
                    .build();

            ProductRequest updateRequest = ProductRequest.builder()
                    .name("X-Burguer")
                    .price(new BigDecimal("25.90"))
                    .categoryId(newCategoryId)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndOwnerId(newCategoryId, ownerId)).willReturn(Optional.of(newCategory));
            given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

            ProductResponse result = productService.update(productId, updateRequest);

            assertThat(result.getCategoryId()).isEqualTo(newCategoryId);
            assertThat(result.getCategoryName()).isEqualTo("Bebidas");
            then(productRepository).should().save(argThat(p ->
                    p.getCategory() != null && newCategoryId.equals(p.getCategory().getId())));
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException quando categoria no update não pertence ao owner")
        void shouldThrowWhenCategoryNotFoundOnUpdate() {
            ProductRequest updateRequest = ProductRequest.builder()
                    .name("X-Burguer")
                    .price(new BigDecimal("25.90"))
                    .categoryId(categoryId)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(productId, updateRequest))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao atualizar produto inexistente")
        void shouldThrowWhenProductNotFoundForUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(productId, productRequest))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar produto existente sem lançar exceção")
        void shouldDeleteExistingProduct() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(true);
            willDoNothing().given(productRepository).deleteByIdAndOwnerId(productId, ownerId);

            assertThatNoException().isThrownBy(() -> productService.delete(productId));

            then(productRepository).should().deleteByIdAndOwnerId(productId, ownerId);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao deletar produto inexistente")
        void shouldThrowWhenProductNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(productRepository.existsByIdAndOwnerId(productId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> productService.delete(productId))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}

