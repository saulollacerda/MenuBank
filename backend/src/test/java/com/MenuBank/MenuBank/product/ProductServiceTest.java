package com.MenuBank.MenuBank.product;

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

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        productRequest = ProductRequest.builder()
                .name("X-Burguer")
                .price(new BigDecimal("25.90"))
                .build();

        product = Product.builder()
                .id(productId)
                .name("X-Burguer")
                .price(new BigDecimal("25.90"))
                .estimatedCost(BigDecimal.ZERO)
                .margin(new BigDecimal("25.90"))
                .status(ProductStatus.ACTIVE)
                .cmv(BigDecimal.ZERO)
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
            given(productRepository.existsByName(productRequest.getName())).willReturn(false);
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo(productRequest.getName());
            assertThat(result.getPrice()).isEqualByComparingTo(productRequest.getPrice());
            then(productRepository).should().save(any(Product.class));
        }

        @Test
        @DisplayName("deve criar produto com status ACTIVE por padrão")
        void shouldCreateProductWithActiveStatusByDefault() {
            given(productRepository.existsByName(anyString())).willReturn(false);
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve criar produto com estimatedCost zero e margin igual ao preço")
        void shouldCreateProductWithZeroCostAndFullMargin() {
            given(productRepository.existsByName(anyString())).willReturn(false);
            given(productRepository.save(any(Product.class))).willReturn(product);

            ProductResponse result = productService.create(productRequest);

            assertThat(result.getEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getMargin()).isEqualByComparingTo(new BigDecimal("25.90"));
            assertThat(result.getCmv()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("deve lançar DuplicateProductException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(productRepository.existsByName(productRequest.getName())).willReturn(true);

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
            given(productRepository.findById(productId)).willReturn(Optional.of(product));

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
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(productId))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista de todos os produtos")
        void shouldReturnListOfAllProducts() {
            given(productRepository.findAll()).willReturn(List.of(product));

            List<ProductResponse> result = productService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(productId);
            assertThat(result.get(0).getName()).isEqualTo("X-Burguer");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há produtos")
        void shouldReturnEmptyList() {
            given(productRepository.findAll()).willReturn(List.of());

            List<ProductResponse> result = productService.findAll();

            assertThat(result).isEmpty();
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
                    .build();

            Product updatedProduct = Product.builder()
                    .id(productId)
                    .name("X-Salada")
                    .price(new BigDecimal("29.90"))
                    .estimatedCost(BigDecimal.ZERO)
                    .margin(new BigDecimal("29.90"))
                    .status(ProductStatus.ACTIVE)
                    .cmv(BigDecimal.ZERO)
                    .build();

            given(productRepository.findById(productId)).willReturn(Optional.of(product));
            given(productRepository.save(any(Product.class))).willReturn(updatedProduct);

            ProductResponse result = productService.update(productId, updateRequest);

            assertThat(result.getName()).isEqualTo("X-Salada");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("29.90"));
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao atualizar produto inexistente")
        void shouldThrowWhenProductNotFoundForUpdate() {
            given(productRepository.findById(productId)).willReturn(Optional.empty());

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
            given(productRepository.existsById(productId)).willReturn(true);
            willDoNothing().given(productRepository).deleteById(productId);

            assertThatNoException().isThrownBy(() -> productService.delete(productId));

            then(productRepository).should().deleteById(productId);
        }

        @Test
        @DisplayName("deve lançar ProductNotFoundException ao deletar produto inexistente")
        void shouldThrowWhenProductNotFoundForDelete() {
            given(productRepository.existsById(productId)).willReturn(false);

            assertThatThrownBy(() -> productService.delete(productId))
                    .isInstanceOf(ProductNotFoundException.class);

            then(productRepository).should(never()).deleteById(any());
        }
    }
}

