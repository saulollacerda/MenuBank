package com.MenuBank.MenuBank.category;

import com.MenuBank.MenuBank.common.UserContext;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private CategoryService categoryService;

    private UUID ownerId;
    private UUID categoryId;
    private Category category;
    private CategoryRequest categoryRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        categoryRequest = CategoryRequest.builder()
                .name("Lanches")
                .build();

        category = Category.builder()
                .id(categoryId)
                .ownerId(ownerId)
                .name("Lanches")
                .build();
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
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.existsByNameAndOwnerId(categoryRequest.getName(), ownerId)).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willReturn(category);

            CategoryResponse result = categoryService.create(categoryRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(categoryId);
            assertThat(result.getName()).isEqualTo(categoryRequest.getName());
            then(categoryRepository).should().save(argThat(c -> ownerId.equals(c.getOwnerId())));
        }

        @Test
        @DisplayName("deve lançar DuplicateCategoryException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.existsByNameAndOwnerId(categoryRequest.getName(), ownerId)).willReturn(true);

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
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));

            CategoryResponse result = categoryService.findById(categoryId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(categoryId);
            assertThat(result.getName()).isEqualTo(category.getName());
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException quando categoria não existe")
        void shouldThrowWhenCategoryNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista de todas as categorias")
        void shouldReturnListOfAllCategories() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.findAllByOwnerId(ownerId)).willReturn(List.of(category));

            List<CategoryResponse> result = categoryService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(categoryId);
            assertThat(result.get(0).getName()).isEqualTo("Lanches");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há categorias")
        void shouldReturnEmptyList() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.findAllByOwnerId(ownerId)).willReturn(List.of());

            List<CategoryResponse> result = categoryService.findAll();

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
        @DisplayName("deve atualizar categoria existente e retornar CategoryResponse atualizado")
        void shouldUpdateAndReturnUpdatedResponse() {
            CategoryRequest updateRequest = CategoryRequest.builder()
                    .name("Bebidas")
                    .build();

            Category updatedCategory = Category.builder()
                    .id(categoryId)
                    .ownerId(ownerId)
                    .name("Bebidas")
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.of(category));
            given(categoryRepository.save(any(Category.class))).willReturn(updatedCategory);

            CategoryResponse result = categoryService.update(categoryId, updateRequest);

            assertThat(result.getName()).isEqualTo("Bebidas");
            assertThat(result.getId()).isEqualTo(categoryId);
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException ao atualizar categoria inexistente")
        void shouldThrowWhenCategoryNotFoundForUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.findByIdAndOwnerId(categoryId, ownerId)).willReturn(Optional.empty());

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
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.existsByIdAndOwnerId(categoryId, ownerId)).willReturn(true);
            willDoNothing().given(categoryRepository).deleteByIdAndOwnerId(categoryId, ownerId);

            assertThatNoException().isThrownBy(() -> categoryService.delete(categoryId));

            then(categoryRepository).should().deleteByIdAndOwnerId(categoryId, ownerId);
        }

        @Test
        @DisplayName("deve lançar CategoryNotFoundException ao deletar categoria inexistente")
        void shouldThrowWhenCategoryNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(categoryRepository.existsByIdAndOwnerId(categoryId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> categoryService.delete(categoryId))
                    .isInstanceOf(CategoryNotFoundException.class);

            then(categoryRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}

