package com.MenuBank.MenuBank.ingredient;

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
@DisplayName("IngredientService")
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    private UUID ingredientId;
    private Ingredient ingredient;
    private IngredientRequest ingredientRequest;

    @BeforeEach
    void setUp() {
        ingredientId = UUID.randomUUID();

        ingredientRequest = IngredientRequest.builder()
                .name("Farinha de Trigo")
                .unit("kg")
                .costPerUnit(new BigDecimal("4.50"))
                .defaultQuantity(new BigDecimal("0.20"))
                .build();

        ingredient = Ingredient.builder()
                .id(ingredientId)
                .name("Farinha de Trigo")
                .unit("kg")
                .costPerUnit(new BigDecimal("4.50"))
                .defaultQuantity(new BigDecimal("0.20"))
                .status(IngredientStatus.ACTIVE)
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar ingrediente com dados válidos e retornar IngredientResponse")
        void shouldCreateIngredientAndReturnResponse() {
            given(ingredientRepository.existsByName(ingredientRequest.getName())).willReturn(false);
            given(ingredientRepository.save(any(Ingredient.class))).willReturn(ingredient);

            IngredientResponse result = ingredientService.create(ingredientRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(ingredientRequest.getName());
            assertThat(result.getUnit()).isEqualTo(ingredientRequest.getUnit());
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(ingredientRequest.getCostPerUnit());
            assertThat(result.getDefaultQuantity()).isEqualByComparingTo(ingredientRequest.getDefaultQuantity());
            then(ingredientRepository).should().save(any(Ingredient.class));
        }

        @Test
        @DisplayName("deve criar ingrediente com status ACTIVE por padrão")
        void shouldCreateIngredientWithActiveStatusByDefault() {
            given(ingredientRepository.existsByName(anyString())).willReturn(false);
            given(ingredientRepository.save(any(Ingredient.class))).willReturn(ingredient);

            IngredientResponse result = ingredientService.create(ingredientRequest);

            assertThat(result.getStatus()).isEqualTo(IngredientStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve lançar DuplicateIngredientException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(ingredientRepository.existsByName(ingredientRequest.getName())).willReturn(true);

            assertThatThrownBy(() -> ingredientService.create(ingredientRequest))
                    .isInstanceOf(DuplicateIngredientException.class)
                    .hasMessageContaining("nome");

            then(ingredientRepository).should(never()).save(any(Ingredient.class));
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar IngredientResponse quando ingrediente existe")
        void shouldReturnResponseWhenExists() {
            given(ingredientRepository.findById(ingredientId)).willReturn(Optional.of(ingredient));

            IngredientResponse result = ingredientService.findById(ingredientId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(ingredientId);
            assertThat(result.getName()).isEqualTo(ingredient.getName());
            assertThat(result.getUnit()).isEqualTo(ingredient.getUnit());
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(ingredient.getCostPerUnit());
            assertThat(result.getDefaultQuantity()).isEqualByComparingTo(ingredient.getDefaultQuantity());
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException quando ingrediente não existe")
        void shouldThrowWhenIngredientNotFound() {
            given(ingredientRepository.findById(ingredientId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ingredientService.findById(ingredientId))
                    .isInstanceOf(IngredientNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista de todos os ingredientes")
        void shouldReturnListOfAllIngredients() {
            given(ingredientRepository.findAll()).willReturn(List.of(ingredient));

            List<IngredientResponse> result = ingredientService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(ingredientId);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há ingredientes")
        void shouldReturnEmptyList() {
            given(ingredientRepository.findAll()).willReturn(List.of());

            List<IngredientResponse> result = ingredientService.findAll();

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
        @DisplayName("deve atualizar ingrediente existente e retornar IngredientResponse atualizado")
        void shouldUpdateAndReturnUpdatedResponse() {
            IngredientRequest updateRequest = IngredientRequest.builder()
                    .name("Farinha de Trigo Integral")
                    .unit("kg")
                    .costPerUnit(new BigDecimal("5.75"))
                    .defaultQuantity(new BigDecimal("0.35"))
                    .build();

            Ingredient updatedIngredient = Ingredient.builder()
                    .id(ingredientId)
                    .name("Farinha de Trigo Integral")
                    .unit("kg")
                    .costPerUnit(new BigDecimal("5.75"))
                    .defaultQuantity(new BigDecimal("0.35"))
                    .status(IngredientStatus.ACTIVE)
                    .build();

            given(ingredientRepository.findById(ingredientId)).willReturn(Optional.of(ingredient));
            given(ingredientRepository.save(any(Ingredient.class))).willReturn(updatedIngredient);

            IngredientResponse result = ingredientService.update(ingredientId, updateRequest);

            assertThat(result.getName()).isEqualTo("Farinha de Trigo Integral");
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(new BigDecimal("5.75"));
            assertThat(result.getDefaultQuantity()).isEqualByComparingTo(new BigDecimal("0.35"));
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException ao atualizar ingrediente inexistente")
        void shouldThrowWhenIngredientNotFoundForUpdate() {
            given(ingredientRepository.findById(ingredientId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ingredientService.update(ingredientId, ingredientRequest))
                    .isInstanceOf(IngredientNotFoundException.class);

            then(ingredientRepository).should(never()).save(any(Ingredient.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar ingrediente existente sem lançar exceção")
        void shouldDeleteExistingIngredient() {
            given(ingredientRepository.existsById(ingredientId)).willReturn(true);
            willDoNothing().given(ingredientRepository).deleteById(ingredientId);

            assertThatNoException().isThrownBy(() -> ingredientService.delete(ingredientId));

            then(ingredientRepository).should().deleteById(ingredientId);
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException ao deletar ingrediente inexistente")
        void shouldThrowWhenIngredientNotFoundForDelete() {
            given(ingredientRepository.existsById(ingredientId)).willReturn(false);

            assertThatThrownBy(() -> ingredientService.delete(ingredientId))
                    .isInstanceOf(IngredientNotFoundException.class);

            then(ingredientRepository).should(never()).deleteById(any());
        }
    }
}
