package com.MenuBank.MenuBank.billing;

import com.MenuBank.MenuBank.integration.abacatepay.AbacatePayClient;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutData;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayCheckoutRequest;
import com.MenuBank.MenuBank.integration.abacatepay.dto.AbacatePayProductRequest;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.merchant.MerchantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbacatePayBillingService")
class AbacatePayBillingServiceTest {

    private static final String FRONTEND_BASE_URL = "https://app.menubank.com.br";

    @Mock
    private AbacatePayClient abacatePayClient;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private AbacatePayBillingService service;

    private UUID merchantId;
    private UUID planId;
    private Plan plan;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        service = new AbacatePayBillingService(
                abacatePayClient, planRepository, subscriptionRepository,
                invoiceRepository, merchantRepository, FRONTEND_BASE_URL);

        merchantId = UUID.randomUUID();
        planId = UUID.randomUUID();

        plan = Plan.builder()
                .id(planId)
                .name("Básico")
                .minRevenue(BigDecimal.ZERO)
                .priceMonthly(new BigDecimal("50.00"))
                .features(Map.of("allFeatures", true))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        merchant = Merchant.builder()
                .id(merchantId)
                .merchantName("Restaurante X")
                .cnpj("12345678000199")
                .email("x@email.com")
                .phone("11999999999")
                .status(MerchantStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // createCheckout()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createCheckout()")
    class CreateCheckout {

        @Test
        @DisplayName("deve criar produto na AbacatePay quando o plano ainda não tem produto e retornar a URL do checkout")
        void shouldCreateProductAndCheckoutWhenPlanHasNoProduct() {
            given(planRepository.findById(planId)).willReturn(Optional.of(plan));
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));
            given(abacatePayClient.createProduct(any(AbacatePayProductRequest.class))).willReturn("prod_abc");

            AbacatePayCheckoutData checkoutData = new AbacatePayCheckoutData();
            checkoutData.setId("bill_xyz");
            checkoutData.setUrl("https://pay.abacatepay.com/bill_xyz");
            given(abacatePayClient.createCheckout(any(AbacatePayCheckoutRequest.class))).willReturn(checkoutData);

            CheckoutResponse response = service.createCheckout(merchantId, planId);

            assertThat(response.getUrl()).isEqualTo("https://pay.abacatepay.com/bill_xyz");

            // product created in cents and persisted on the plan
            then(abacatePayClient).should().createProduct(argThat(req ->
                    req.getPrice() == 5000L && "BRL".equals(req.getCurrency())));
            then(planRepository).should().save(argThat(p -> "prod_abc".equals(p.getAbacatepayProductId())));

            ArgumentCaptor<AbacatePayCheckoutRequest> captor =
                    ArgumentCaptor.forClass(AbacatePayCheckoutRequest.class);
            then(abacatePayClient).should().createCheckout(captor.capture());
            AbacatePayCheckoutRequest sent = captor.getValue();

            assertThat(sent.getItems()).hasSize(1);
            assertThat(sent.getItems().get(0).getId()).isEqualTo("prod_abc");
            assertThat(sent.getItems().get(0).getQuantity()).isEqualTo(1);
            assertThat(sent.getCustomer().getName()).isEqualTo("Restaurante X");
            assertThat(sent.getCustomer().getEmail()).isEqualTo("x@email.com");
            assertThat(sent.getCustomer().getTaxId()).isEqualTo("12345678000199");
            assertThat(sent.getExternalId()).isEqualTo("menubank:" + merchantId + ":" + planId);
            assertThat(sent.getReturnUrl()).startsWith(FRONTEND_BASE_URL);
            assertThat(sent.getCompletionUrl()).startsWith(FRONTEND_BASE_URL);
        }

        @Test
        @DisplayName("deve reutilizar o produto existente quando o plano já tem abacatepayProductId")
        void shouldReuseExistingProduct() {
            plan.setAbacatepayProductId("prod_existing");
            given(planRepository.findById(planId)).willReturn(Optional.of(plan));
            given(merchantRepository.findById(merchantId)).willReturn(Optional.of(merchant));

            AbacatePayCheckoutData checkoutData = new AbacatePayCheckoutData();
            checkoutData.setId("bill_xyz");
            checkoutData.setUrl("https://pay.abacatepay.com/bill_xyz");
            given(abacatePayClient.createCheckout(any(AbacatePayCheckoutRequest.class))).willReturn(checkoutData);

            service.createCheckout(merchantId, planId);

            then(abacatePayClient).should(never()).createProduct(any());
            then(abacatePayClient).should().createCheckout(argThat(req ->
                    "prod_existing".equals(req.getItems().get(0).getId())));
        }

        @Test
        @DisplayName("deve lançar PlanNotFoundException quando o plano não existe")
        void shouldThrowWhenPlanNotFound() {
            given(planRepository.findById(planId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createCheckout(merchantId, planId))
                    .isInstanceOf(PlanNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // handleBillingPaid()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleBillingPaid()")
    class HandleBillingPaid {

        @Test
        @DisplayName("deve ativar a assinatura por 1 mês e registrar fatura paga")
        void shouldActivateSubscriptionAndCreatePaidInvoice() {
            Subscription subscription = Subscription.builder()
                    .id(UUID.randomUUID())
                    .merchantId(merchantId)
                    .status(SubscriptionStatus.TRIAL)
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .updatedAt(LocalDateTime.now().minusDays(3))
                    .build();

            given(invoiceRepository.findByAbacatepayBillingId("bill_xyz")).willReturn(Optional.empty());
            given(subscriptionRepository.findByMerchantId(merchantId)).willReturn(Optional.of(subscription));
            given(planRepository.findById(planId)).willReturn(Optional.of(plan));

            service.handleBillingPaid("bill_xyz", "menubank:" + merchantId + ":" + planId, 5000L);

            then(subscriptionRepository).should().save(argThat(sub ->
                    SubscriptionStatus.ACTIVE.equals(sub.getStatus())
                            && plan.equals(sub.getPlan())
                            && sub.getCurrentPeriodStart() != null
                            && sub.getCurrentPeriodEnd() != null
                            && sub.getCurrentPeriodEnd().isAfter(LocalDateTime.now().plusDays(27))
            ));

            then(invoiceRepository).should().save(argThat(invoice ->
                    InvoiceStatus.PAID.equals(invoice.getStatus())
                            && new BigDecimal("50.00").compareTo(invoice.getAmount()) == 0
                            && "bill_xyz".equals(invoice.getAbacatepayBillingId())
                            && invoice.getPaidAt() != null
            ));
        }

        @Test
        @DisplayName("deve ignorar evento já processado (mesmo billing id)")
        void shouldBeIdempotentForAlreadyProcessedBilling() {
            given(invoiceRepository.findByAbacatepayBillingId("bill_xyz"))
                    .willReturn(Optional.of(Invoice.builder().abacatepayBillingId("bill_xyz").build()));

            service.handleBillingPaid("bill_xyz", "menubank:" + merchantId + ":" + planId, 5000L);

            then(subscriptionRepository).should(never()).save(any());
            then(invoiceRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("deve ignorar externalId que não segue o formato menubank:<merchantId>:<planId>")
        void shouldIgnoreUnknownExternalId() {
            given(invoiceRepository.findByAbacatepayBillingId("bill_other")).willReturn(Optional.empty());

            service.handleBillingPaid("bill_other", "other-system-42", 5000L);

            then(subscriptionRepository).should(never()).save(any());
            then(invoiceRepository).should(never()).save(any());
        }
    }
}
