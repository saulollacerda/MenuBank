package com.MenuBank.MenuBank.payment;

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
@DisplayName("PaymentMethodService")
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    private UUID ownerId;
    private UUID paymentMethodId;
    private PaymentMethod paymentMethod;
    private PaymentMethodRequest request;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        paymentMethodId = UUID.randomUUID();

        request = PaymentMethodRequest.builder()
                .name("Crédito")
                .feeRate(new BigDecimal("2.5000"))
                .build();

        paymentMethod = PaymentMethod.builder()
                .id(paymentMethodId)
                .ownerId(ownerId)
                .name("Crédito")
                .feeRate(new BigDecimal("2.5000"))
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar forma de pagamento com dados válidos e retornar PaymentMethodResponse")
        void shouldCreateAndReturnResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.existsByNameAndOwnerId(request.getName(), ownerId)).willReturn(false);
            given(paymentMethodRepository.save(any(PaymentMethod.class))).willReturn(paymentMethod);

            PaymentMethodResponse result = paymentMethodService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(paymentMethodId);
            assertThat(result.getName()).isEqualTo("Crédito");
            assertThat(result.getFeeRate()).isEqualByComparingTo(new BigDecimal("2.5000"));
            then(paymentMethodRepository).should().save(argThat(pm -> ownerId.equals(pm.getOwnerId())));
        }

        @Test
        @DisplayName("deve lançar DuplicatePaymentMethodException quando nome já está cadastrado")
        void shouldThrowWhenNameAlreadyExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.existsByNameAndOwnerId(request.getName(), ownerId)).willReturn(true);

            assertThatThrownBy(() -> paymentMethodService.create(request))
                    .isInstanceOf(DuplicatePaymentMethodException.class);

            then(paymentMethodRepository).should(never()).save(any(PaymentMethod.class));
        }

        @Test
        @DisplayName("deve permitir criar forma de pagamento com taxa zero (ex: Dinheiro)")
        void shouldAllowZeroFeeRate() {
            PaymentMethodRequest cashRequest = PaymentMethodRequest.builder()
                    .name("Dinheiro")
                    .feeRate(BigDecimal.ZERO)
                    .build();
            PaymentMethod cash = PaymentMethod.builder()
                    .id(paymentMethodId)
                    .ownerId(ownerId)
                    .name("Dinheiro")
                    .feeRate(BigDecimal.ZERO)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.existsByNameAndOwnerId("Dinheiro", ownerId)).willReturn(false);
            given(paymentMethodRepository.save(any(PaymentMethod.class))).willReturn(cash);

            PaymentMethodResponse result = paymentMethodService.create(cashRequest);

            assertThat(result.getFeeRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar PaymentMethodResponse quando forma de pagamento existe")
        void shouldReturnResponseWhenExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.findByIdAndOwnerId(paymentMethodId, ownerId))
                    .willReturn(Optional.of(paymentMethod));

            PaymentMethodResponse result = paymentMethodService.findById(paymentMethodId);

            assertThat(result.getId()).isEqualTo(paymentMethodId);
            assertThat(result.getName()).isEqualTo("Crédito");
        }

        @Test
        @DisplayName("deve lançar PaymentMethodNotFoundException quando não encontrada")
        void shouldThrowWhenNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.findByIdAndOwnerId(paymentMethodId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentMethodService.findById(paymentMethodId))
                    .isInstanceOf(PaymentMethodNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll(search, pageable)")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de formas de pagamento filtrada por nome (contains, case-insensitive)")
        void shouldReturnPagedFilteredByName() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, "cred", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(paymentMethod), pageable, 1));

            org.springframework.data.domain.Page<PaymentMethodResponse> result =
                    paymentMethodService.findAll("cred", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Crédito");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve tratar search nulo como string vazia")
        void shouldTreatNullSearchAsEmpty() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));

            org.springframework.data.domain.Page<PaymentMethodResponse> result =
                    paymentMethodService.findAll(null, pageable);

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
        @DisplayName("deve atualizar forma de pagamento e retornar resposta atualizada")
        void shouldUpdateAndReturnUpdatedResponse() {
            PaymentMethodRequest updateRequest = PaymentMethodRequest.builder()
                    .name("Débito")
                    .feeRate(new BigDecimal("1.0000"))
                    .build();

            PaymentMethod updated = PaymentMethod.builder()
                    .id(paymentMethodId)
                    .ownerId(ownerId)
                    .name("Débito")
                    .feeRate(new BigDecimal("1.0000"))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.findByIdAndOwnerId(paymentMethodId, ownerId))
                    .willReturn(Optional.of(paymentMethod));
            given(paymentMethodRepository.save(any(PaymentMethod.class))).willReturn(updated);

            PaymentMethodResponse result = paymentMethodService.update(paymentMethodId, updateRequest);

            assertThat(result.getName()).isEqualTo("Débito");
            assertThat(result.getFeeRate()).isEqualByComparingTo(new BigDecimal("1.0000"));
        }

        @Test
        @DisplayName("deve lançar PaymentMethodNotFoundException ao atualizar forma inexistente")
        void shouldThrowWhenNotFoundForUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.findByIdAndOwnerId(paymentMethodId, ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentMethodService.update(paymentMethodId, request))
                    .isInstanceOf(PaymentMethodNotFoundException.class);

            then(paymentMethodRepository).should(never()).save(any(PaymentMethod.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar forma de pagamento existente sem lançar exceção")
        void shouldDeleteExisting() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.existsByIdAndOwnerId(paymentMethodId, ownerId)).willReturn(true);
            willDoNothing().given(paymentMethodRepository).deleteByIdAndOwnerId(paymentMethodId, ownerId);

            assertThatNoException().isThrownBy(() -> paymentMethodService.delete(paymentMethodId));

            then(paymentMethodRepository).should().deleteByIdAndOwnerId(paymentMethodId, ownerId);
        }

        @Test
        @DisplayName("deve lançar PaymentMethodNotFoundException ao deletar forma inexistente")
        void shouldThrowWhenNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(paymentMethodRepository.existsByIdAndOwnerId(paymentMethodId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> paymentMethodService.delete(paymentMethodId))
                    .isInstanceOf(PaymentMethodNotFoundException.class);

            then(paymentMethodRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}
