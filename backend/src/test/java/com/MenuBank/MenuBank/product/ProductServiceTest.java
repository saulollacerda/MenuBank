package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryNotFoundException;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.common.MerchantContext;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MerchantContext merchantContext;

    @Mock
    private MerchantRepository merchantRepository;


    @InjectMocks
    private ProductService productService;

    private UUID merchantId;
    private UUID productId;
    private UUID categoryId;
    private Category category;
    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        lenient().when(merchantRepository.getReferenceById(any())).thenReturn(Merchant.builder().id(merchantId).build());
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .merchant(Merchant.builder().id(merchantId).build())
                .name("Lanches")
                .build();

        productRequest = ProductRequest.builder()
                .name("X-Burguer")
                .price(new BigDecimal("25.90"))
                .categoryId(categoryId)
                .build();

        product = Product.builder()
                .id(productId)
                .merchant(Merchant.builder().id(merchantId).build())
                .name("X-Burguer")
                .price(new BigDecimal("25.90"))
                .status(ProductStatus.ACTIVE)
                .category(category)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar produto com dados válidos e retornar ProductResponse")
        void shouldCreateProductAndReturnResponse() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByNameAndMerchantId(productRequest.getName(), merchantId)).willReturn(false);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo(productRequest.getName());
            assertThat(result.getPrice()).isEqualByComparingTo(productRequest.getPrice());
            then(productRepository).should().save(argThat(p -> merchantId.equals(p.getMerchant().getId())));
        }

        @Test
        @DisplayName("deve criar produto com status ACTIVE por padrão")
        void shouldCreateProductWithActiveStatusByDefault() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByNameAndMerchantId(anyString(), eq(merchantId))).willReturn(false);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve atribuir a categoria ao produto e retornar categoryId/categoryName na resposta")
        void shouldAssignCategoryAndReturnCategoryFieldsInResponse() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByNameAndMerchantId(anyString(), eq(merchantId))).willReturn(false);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.of(category));
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
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByNameAndMerchantId(anyString(), eq(merchantId))).willReturn(false);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(productRequest))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("deve lançar DuplicateProductException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByNameAndMerchantId(productRequest.getName(), merchantId)).willReturn(true);

            assertThatThrownBy(() -> productService.create(productRequest))
                    .isInstanceOf(DuplicateProductException.class)
                    .hasMessageContaining("nome");

            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar ProductResponse quando produto existe")
        void shouldReturnResponseWhenExists() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));

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
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll(search, pageable)")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de produtos filtrada por nome (contains, case-insensitive)")
        void shouldReturnPagedProductsFilteredByName() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, "burg", pageable))
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
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(product), pageable, 1));

            org.springframework.data.domain.Page<ProductResponse> result =
                    productService.findAll(null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

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
                    .merchant(Merchant.builder().id(merchantId).build())
                    .name("X-Salada")
                    .price(new BigDecimal("29.90"))
                    .status(ProductStatus.ACTIVE)
                    .category(category)
                    .build();

            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.of(category));
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
                    .merchant(Merchant.builder().id(merchantId).build())
                    .name("Bebidas")
                    .build();

            ProductRequest updateRequest = ProductRequest.builder()
                    .name("X-Burguer")
                    .price(new BigDecimal("25.90"))
                    .categoryId(newCategoryId)
                    .build();

            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndMerchantId(newCategoryId, merchantId)).willReturn(Optional.of(newCategory));
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

            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(productId, updateRequest))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(productRepository).should(never()).save(any(Product.class));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao atualizar produto inexistente")
        void shouldThrowWhenProductNotFoundForUpdate() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(productId, productRequest))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productRepository).should(never()).save(any(Product.class));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar produto existente sem lançar exceção")
        void shouldDeleteExistingProduct() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByIdAndMerchantId(productId, merchantId)).willReturn(true);
            willDoNothing().given(productRepository).deleteByIdAndMerchantId(productId, merchantId);

            assertThatNoException().isThrownBy(() -> productService.delete(productId));

            then(productRepository).should().deleteByIdAndMerchantId(productId, merchantId);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao deletar produto inexistente")
        void shouldThrowWhenProductNotFoundForDelete() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(productRepository.existsByIdAndMerchantId(productId, merchantId)).willReturn(false);

            assertThatThrownBy(() -> productService.delete(productId))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productRepository).should(never()).deleteByIdAndMerchantId(any(), any());
        }
    }
}
