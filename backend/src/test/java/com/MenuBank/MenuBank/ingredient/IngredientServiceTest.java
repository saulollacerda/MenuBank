package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.notification.NotificationService;
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

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private IngredientService ingredientService;

    private UUID ownerId;
    private UUID ingredientId;
    private Ingredient ingredient;
    private IngredientRequest ingredientRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();

        ingredientRequest = IngredientRequest.builder()
                .name("Farinha de Trigo")
                .unit("kg")
                .costPerUnit(new BigDecimal("4.50"))
                .defaultQuantity(new BigDecimal("0.20"))
                .build();

        ingredient = Ingredient.builder()
                .id(ingredientId)
                .ownerId(ownerId)
                .name("Farinha de Trigo")
                .canonicalName("farinha de trigo")
                .unit("kg")
                .costPerUnit(new BigDecimal("4.50"))
                .defaultQuantity(new BigDecimal("0.20"))
                .status(IngredientStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar ingrediente com dados válidos e retornar IngredientResponse")
        void shouldCreateIngredientAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByNameAndOwnerId(ingredientRequest.getName(), ownerId)).willReturn(false);
            given(ingredientRepository.save(any(Ingredient.class))).willReturn(ingredient);

            IngredientResponse result = ingredientService.create(ingredientRequest);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(ingredientRequest.getName());
            assertThat(result.getUnit()).isEqualTo(ingredientRequest.getUnit());
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(ingredientRequest.getCostPerUnit());
            assertThat(result.getDefaultQuantity()).isEqualByComparingTo(ingredientRequest.getDefaultQuantity());
            then(ingredientRepository).should().save(argThat(i -> ownerId.equals(i.getOwnerId())));
        }

        @Test
        @DisplayName("deve criar ingrediente com status ACTIVE por padrão")
        void shouldCreateIngredientWithActiveStatusByDefault() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(ingredientRepository.save(any(Ingredient.class))).willReturn(ingredient);

            IngredientResponse result = ingredientService.create(ingredientRequest);

            assertThat(result.getStatus()).isEqualTo(IngredientStatus.ACTIVE);
        }

        @Test
        @DisplayName("deve lançar DuplicateIngredientException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByNameAndOwnerId(ingredientRequest.getName(), ownerId)).willReturn(true);

            assertThatThrownBy(() -> ingredientService.create(ingredientRequest))
                    .isInstanceOf(DuplicateIngredientException.class)
                    .hasMessageContaining("nome");

            then(ingredientRepository).should(never()).save(any(Ingredient.class));
        }

        @Test
        @DisplayName("deve popular canonicalName normalizado a partir do nome (lowercase, sem acentos)")
        void shouldPopulateCanonicalNameNormalizedFromName() {
            IngredientRequest withAccent = IngredientRequest.builder()
                    .name("Açaí Premium").unit("ml")
                    .costPerUnit(new BigDecimal("0.05"))
                    .build();
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(ingredientRepository.save(any(Ingredient.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ingredientService.create(withAccent);

            then(ingredientRepository).should()
                    .save(argThat(i -> "acai premium".equals(i.getCanonicalName())));
        }

        @Test
        @DisplayName("deve resolver notificações pendentes 'MISSING_INGREDIENT' do canonical name após salvar")
        void shouldResolvePendingMissingIngredientNotificationsAfterSave() {
            IngredientRequest withAccent = IngredientRequest.builder()
                    .name("Açaí Premium").unit("ml")
                    .costPerUnit(new BigDecimal("0.05"))
                    .build();
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByNameAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
            given(ingredientRepository.save(any(Ingredient.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            ingredientService.create(withAccent);

            then(notificationService).should().resolveMissingIngredient("acai premium", ownerId);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar IngredientResponse quando ingrediente existe")
        void shouldReturnResponseWhenExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));

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
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ingredientService.findById(ingredientId))
                    .isInstanceOf(IngredientNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll(search, pageable)")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de ingredientes filtrada por nome (contains, case-insensitive)")
        void shouldReturnPagedIngredientsFilteredByName() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, "bac", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(ingredient), pageable, 1));

            org.springframework.data.domain.Page<IngredientResponse> result =
                    ingredientService.findAll("bac", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(ingredientId);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve tratar search nulo como string vazia")
        void shouldTreatNullSearchAsEmpty() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));

            org.springframework.data.domain.Page<IngredientResponse> result =
                    ingredientService.findAll(null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

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
                    .ownerId(ownerId)
                    .name("Farinha de Trigo Integral")
                    .canonicalName("farinha de trigo integral")
                    .unit("kg")
                    .costPerUnit(new BigDecimal("5.75"))
                    .defaultQuantity(new BigDecimal("0.35"))
                    .status(IngredientStatus.ACTIVE)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(ingredientRepository.save(any(Ingredient.class))).willReturn(updatedIngredient);

            IngredientResponse result = ingredientService.update(ingredientId, updateRequest);

            assertThat(result.getName()).isEqualTo("Farinha de Trigo Integral");
            assertThat(result.getCostPerUnit()).isEqualByComparingTo(new BigDecimal("5.75"));
            assertThat(result.getDefaultQuantity()).isEqualByComparingTo(new BigDecimal("0.35"));
        }

        @Test
        @DisplayName("deve recomputar canonicalName ao alterar nome")
        void shouldRecomputeCanonicalNameOnUpdate() {
            IngredientRequest updateRequest = IngredientRequest.builder()
                    .name("PISTACHE")
                    .unit("g")
                    .costPerUnit(new BigDecimal("1.20"))
                    .build();
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(ingredientRepository.save(any(Ingredient.class))).willAnswer(inv -> inv.getArgument(0));

            ingredientService.update(ingredientId, updateRequest);

            then(ingredientRepository).should()
                    .save(argThat(i -> "pistache".equals(i.getCanonicalName())));
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException ao atualizar ingrediente inexistente")
        void shouldThrowWhenIngredientNotFoundForUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ingredientService.update(ingredientId, ingredientRequest))
                    .isInstanceOf(IngredientNotFoundException.class);

            then(ingredientRepository).should(never()).save(any(Ingredient.class));
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar ingrediente existente sem lançar exceção")
        void shouldDeleteExistingIngredient() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByIdAndOwnerId(ingredientId, ownerId)).willReturn(true);
            willDoNothing().given(ingredientRepository).deleteByIdAndOwnerId(ingredientId, ownerId);

            assertThatNoException().isThrownBy(() -> ingredientService.delete(ingredientId));

            then(ingredientRepository).should().deleteByIdAndOwnerId(ingredientId, ownerId);
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException ao deletar ingrediente inexistente")
        void shouldThrowWhenIngredientNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(ingredientRepository.existsByIdAndOwnerId(ingredientId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> ingredientService.delete(ingredientId))
                    .isInstanceOf(IngredientNotFoundException.class);

            then(ingredientRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}
