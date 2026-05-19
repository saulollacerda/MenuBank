package com.MenuBank.MenuBank.ingredient;

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
@DisplayName("IngredientCategoryService")
class IngredientCategoryServiceTest {

    @Mock
    private IngredientCategoryRepository ingredientCategoryRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private IngredientCategoryService ingredientCategoryService;

    private UUID ownerId;
    private UUID categoryId;
    private IngredientCategory category;
    private IngredientCategoryRequest request;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        request = IngredientCategoryRequest.builder()
                .name("Adicionais de creme")
                .build();

        category = IngredientCategory.builder()
                .id(categoryId)
                .ownerId(ownerId)
                .name("Adicionais de creme")
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar categoria de ingrediente e retornar response")
        void shouldCreateAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.existsByNameAndOwnerId(request.getName(), ownerId)).willReturn(false);
            given(ingredientCategoryRepository.save(any(IngredientCategory.class))).willReturn(category);

            IngredientCategoryResponse result = ingredientCategoryService.create(request);

            assertThat(result.getId()).isEqualTo(categoryId);
            assertThat(result.getName()).isEqualTo("Adicionais de creme");
            then(ingredientCategoryRepository).should().save(argThat(c -> ownerId.equals(c.getOwnerId())));
        }

        @Test
        @DisplayName("deve lançar DuplicateIngredientCategoryException quando nome já existe")
        void shouldThrowWhenNameAlreadyExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.existsByNameAndOwnerId(request.getName(), ownerId)).willReturn(true);

            assertThatThrownBy(() -> ingredientCategoryService.create(request))
                    .isInstanceOf(DuplicateIngredientCategoryException.class);

            then(ingredientCategoryRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar todas as categorias do owner")
        void shouldReturnAllForOwner() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.findAllByOwnerId(ownerId)).willReturn(List.of(category));

            List<IngredientCategoryResponse> result = ingredientCategoryService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Adicionais de creme");
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há categorias")
        void shouldReturnEmptyList() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.findAllByOwnerId(ownerId)).willReturn(List.of());

            assertThat(ingredientCategoryService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("deve atualizar nome da categoria")
        void shouldUpdateName() {
            IngredientCategoryRequest updateRequest = IngredientCategoryRequest.builder()
                    .name("Cremes especiais")
                    .build();

            IngredientCategory updated = IngredientCategory.builder()
                    .id(categoryId).ownerId(ownerId).name("Cremes especiais").build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.findByIdAndOwnerId(categoryId, ownerId))
                    .willReturn(Optional.of(category));
            given(ingredientCategoryRepository.save(any(IngredientCategory.class))).willReturn(updated);

            IngredientCategoryResponse result = ingredientCategoryService.update(categoryId, updateRequest);

            assertThat(result.getName()).isEqualTo("Cremes especiais");
        }

        @Test
        @DisplayName("deve lançar IngredientCategoryNotFoundException quando categoria não existe")
        void shouldThrowWhenNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.findByIdAndOwnerId(categoryId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> ingredientCategoryService.update(categoryId, request))
                    .isInstanceOf(IngredientCategoryNotFoundException.class);

            then(ingredientCategoryRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar categoria existente")
        void shouldDeleteExistingCategory() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.existsByIdAndOwnerId(categoryId, ownerId)).willReturn(true);
            willDoNothing().given(ingredientCategoryRepository).deleteByIdAndOwnerId(categoryId, ownerId);

            assertThatNoException().isThrownBy(() -> ingredientCategoryService.delete(categoryId));

            then(ingredientCategoryRepository).should().deleteByIdAndOwnerId(categoryId, ownerId);
        }

        @Test
        @DisplayName("deve lançar IngredientCategoryNotFoundException quando categoria não existe")
        void shouldThrowWhenNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientCategoryRepository.existsByIdAndOwnerId(categoryId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> ingredientCategoryService.delete(categoryId))
                    .isInstanceOf(IngredientCategoryNotFoundException.class);

            then(ingredientCategoryRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}
