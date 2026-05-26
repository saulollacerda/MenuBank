package com.MenuBank.MenuBank.category;

import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;

import com.MenuBank.MenuBank.common.MerchantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private MerchantContext merchantContext;

    @InjectMocks
    private CategoryService categoryService;

    private UUID merchantId;
    private UUID categoryId;
    private Category category;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        categoryRequest = CategoryRequest.builder()
                .name("Lanches")
                .build();

        category = Category.builder()
                .id(categoryId)
                .merchant(Merchant.builder().id(merchantId).build())
                .name("Lanches")
                .build();

        lenient().when(merchantRepository.getReferenceById(any()))
                .thenReturn(Merchant.builder().id(merchantId).build());
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar categoria com dados válidos e retornar CategoryResponse")
        void shouldCreateCategoryAndReturnResponse() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.existsByNameAndMerchantId(categoryRequest.getName(), merchantId)).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willReturn(category);

            CategoryResponse result = categoryService.create(categoryRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(categoryId);
            assertThat(result.getName()).isEqualTo(categoryRequest.getName());
            then(categoryRepository).should().save(argThat(c -> merchantId.equals(c.getMerchant().getId())));
        }

        @Test
        @DisplayName("deve lançar DuplicateCategoryException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.existsByNameAndMerchantId(categoryRequest.getName(), merchantId)).willReturn(true);

            assertThatThrownBy(() -> categoryService.create(categoryRequest))
                    .isInstanceOf(DuplicateCategoryException.class)
                    .hasMessageContaining("nome");

            then(categoryRepository).should(never()).save(any(Category.class));
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar CategoryResponse quando categoria existe")
        void shouldReturnResponseWhenExists() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.of(category));

            CategoryResponse result = categoryService.findById(categoryId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(categoryId);
            assertThat(result.getName()).isEqualTo(category.getName());
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException quando categoria não existe")
        void shouldThrowWhenCategoryNotFound() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll(search, pageable)")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de categorias filtrada por nome (contains, case-insensitive)")
        void shouldReturnPagedCategoriesFilteredByName() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, "lan", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(category), pageable, 1));

            org.springframework.data.domain.Page<CategoryResponse> result =
                    categoryService.findAll("lan", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Lanches");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve tratar search nulo como string vazia")
        void shouldTreatNullSearchAsEmpty() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));

            org.springframework.data.domain.Page<CategoryResponse> result =
                    categoryService.findAll(null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar categoria existente e retornar CategoryResponse atualizado")
        void shouldUpdateAndReturnUpdatedResponse() {
            CategoryRequest updateRequest = CategoryRequest.builder()
                    .name("Bebidas")
                    .build();

            Category updatedCategory = Category.builder()
                    .id(categoryId)
                    .merchant(Merchant.builder().id(merchantId).build())
                    .name("Bebidas")
                    .build();

            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.of(category));
            given(categoryRepository.save(any(Category.class))).willReturn(updatedCategory);

            CategoryResponse result = categoryService.update(categoryId, updateRequest);

            assertThat(result.getName()).isEqualTo("Bebidas");
            assertThat(result.getId()).isEqualTo(categoryId);
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException ao atualizar categoria inexistente")
        void shouldThrowWhenCategoryNotFoundForUpdate() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.findByIdAndMerchantId(categoryId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(categoryId, categoryRequest))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(categoryRepository).should(never()).save(any(Category.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar categoria existente sem lançar exceção")
        void shouldDeleteExistingCategory() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.existsByIdAndMerchantId(categoryId, merchantId)).willReturn(true);
            willDoNothing().given(categoryRepository).deleteByIdAndMerchantId(categoryId, merchantId);

            assertThatNoException().isThrownBy(() -> categoryService.delete(categoryId));

            then(categoryRepository).should().deleteByIdAndMerchantId(categoryId, merchantId);
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException ao deletar categoria inexistente")
        void shouldThrowWhenCategoryNotFoundForDelete() {
            given(merchantContext.getMerchantId()).willReturn(merchantId);
            given(categoryRepository.existsByIdAndMerchantId(categoryId, merchantId)).willReturn(false);

            assertThatThrownBy(() -> categoryService.delete(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(categoryRepository).should(never()).deleteByIdAndMerchantId(any(), any());
        }
    }
}

