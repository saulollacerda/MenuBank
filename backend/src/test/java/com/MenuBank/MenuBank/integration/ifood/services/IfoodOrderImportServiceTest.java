package com.MenuBank.MenuBank.integration.ifood.services;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.integration.ifood.dto.IfoodOrderDetailResponse;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.OrderCostCalculatorService;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("IfoodOrderImportService")
class IfoodOrderImportServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private IngredientRepository ingredientRepository;
    @Mock private IncludeRepository includeRepository;
    @Mock private NotificationService notificationService;
    @Mock private OrderCostCalculatorService orderCostCalculatorService;

    private IfoodOrderImportService importService;

    private UUID merchantId;
    private Merchant merchant;
    private Product product;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        merchant = Merchant.builder().id(merchantId).ifoodMerchantId("ifood-m1").build();

        product = Product.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("Açaí 500 ml")
                .canonicalName("acai 500 ml")
                .price(new BigDecimal("21.99"))
                .build();

        importService = new IfoodOrderImportService(
                merchantRepository, orderRepository, customerRepository, productRepository,
                ingredientRepository, includeRepository, notificationService, orderCostCalculatorService);

        lenient().when(merchantRepository.findByIfoodMerchantId("ifood-m1")).thenReturn(Optional.of(merchant));
        lenient().when(merchantRepository.getReferenceById(merchantId)).thenReturn(merchant);
        lenient().when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                .thenReturn(BigDecimal.ZERO);
        lenient().when(includeRepository.findByProductIdAndProductMerchantId(any(), any()))
                .thenReturn(List.of());
    }

    private IfoodOrderDetailResponse baseDetail() {
        IfoodOrderDetailResponse detail = new IfoodOrderDetailResponse();
        detail.setId("ord-1");
        detail.setCategory("FOOD");
        detail.setTest(false);
        detail.setCreatedAt("2026-07-01T18:00:00Z");
        detail.setExtraInfo("Pago Online");

        IfoodOrderDetailResponse.MerchantInfo merchantInfo = new IfoodOrderDetailResponse.MerchantInfo();
        merchantInfo.setId("ifood-m1");
        merchantInfo.setName("Loja");
        detail.setMerchant(merchantInfo);

        IfoodOrderDetailResponse.CustomerInfo customer = new IfoodOrderDetailResponse.CustomerInfo();
        customer.setName("Maria Santos");
        IfoodOrderDetailResponse.Phone phone = new IfoodOrderDetailResponse.Phone();
        phone.setNumber("11999998888");
        customer.setPhone(phone);
        detail.setCustomer(customer);

        IfoodOrderDetailResponse.Item item = new IfoodOrderDetailResponse.Item();
        item.setExternalCode("PDV-1");
        item.setName("Açaí 500 ml");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("21.99"));
        detail.setItems(List.of(item));

        IfoodOrderDetailResponse.Total total = new IfoodOrderDetailResponse.Total();
        total.setSubTotal(new BigDecimal("43.98"));
        total.setDeliveryFee(new BigDecimal("5.99"));
        total.setOrderAmount(new BigDecimal("49.97"));
        detail.setTotal(total);

        return detail;
    }

    @Nested
    @DisplayName("importOrder()")
    class ImportOrder {

        @Test
        @DisplayName("deve importar pedido FOOD concluído com origin=IFOOD, o status do evento e extraInfo")
        void shouldImportConcludedFoodOrder() {
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(baseDetail(), OrderStatus.PAID);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getOrigin() == OrderOrigin.IFOOD
                            && o.getStatus() == OrderStatus.PAID
                            && "ord-1".equals(o.getExternalOrderId())
                            && "Pago Online".equals(o.getExtraInfo())
                            && new BigDecimal("49.97").compareTo(o.getTotalValue()) == 0
                            && new BigDecimal("5.99").compareTo(o.getDeliveryFee()) == 0
                            && o.getFee() == null
                            && o.getItems().size() == 1
                            && new BigDecimal("21.99").compareTo(o.getItems().get(0).getUnitPrice()) == 0));
        }

        @Test
        @DisplayName("deve pular pedido com category diferente de FOOD")
        void shouldSkipNonFoodCategory() {
            IfoodOrderDetailResponse detail = baseDetail();
            detail.setCategory("GROCERY");

            boolean imported = importService.importOrder(detail, OrderStatus.PAID);

            assertThat(imported).isFalse();
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve importar pedido de teste (isTest=true) com status TEST, ignorando o status do evento")
        void shouldImportTestOrderWithTestStatus() {
            IfoodOrderDetailResponse detail = baseDetail();
            detail.setTest(true);
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(detail, OrderStatus.PAID);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getStatus() == OrderStatus.TEST));
        }

        @Test
        @DisplayName("deve pular pedido de merchant desconhecido (ifoodMerchantId não autorizado)")
        void shouldSkipUnknownMerchant() {
            given(merchantRepository.findByIfoodMerchantId("ifood-m1")).willReturn(Optional.empty());

            boolean imported = importService.importOrder(baseDetail(), OrderStatus.PAID);

            assertThat(imported).isFalse();
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve pular pedido duplicado (externalOrderId já importado)")
        void shouldSkipDuplicatedOrder() {
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(true);

            boolean imported = importService.importOrder(baseDetail(), OrderStatus.PAID);

            assertThat(imported).isFalse();
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve resolver produto por nome canônico quando externalCode não casa")
        void shouldResolveProductByCanonicalNameFallback() {
            IfoodOrderDetailResponse detail = baseDetail();
            detail.getItems().get(0).setExternalCode(null);
            detail.getItems().get(0).setName("AÇAÍ  500 ML");

            given(productRepository.findByCanonicalNameAndMerchantId("acai 500 ml", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(detail, OrderStatus.PAID);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) -> o.getItems().size() == 1));
        }

        @Test
        @DisplayName("deve pular item sem produto correspondente (nem código nem nome) mas importar o pedido")
        void shouldSkipItemWithoutMatchingProduct() {
            IfoodOrderDetailResponse detail = baseDetail();
            IfoodOrderDetailResponse.Item unknown = new IfoodOrderDetailResponse.Item();
            unknown.setExternalCode("NOPE");
            unknown.setName("Produto Fantasma");
            unknown.setQuantity(BigDecimal.ONE);
            unknown.setUnitPrice(BigDecimal.TEN);
            detail.setItems(List.of(detail.getItems().get(0), unknown));

            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(productRepository.findByExternalIdAndMerchantId("NOPE", merchantId))
                    .willReturn(Optional.empty());
            given(productRepository.findByCanonicalNameAndMerchantId("produto fantasma", merchantId))
                    .willReturn(Optional.empty());
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(detail, OrderStatus.PAID);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) -> o.getItems().size() == 1));
        }

        @Test
        @DisplayName("deve notificar MISSING_PRODUCT quando item não casa com nenhum produto")
        void shouldNotifyMissingProductWhenItemHasNoMatch() {
            IfoodOrderDetailResponse detail = baseDetail();
            detail.getItems().get(0).setExternalCode("NOPE");
            detail.getItems().get(0).setName("Produto Fantasma");

            given(productRepository.findByExternalIdAndMerchantId("NOPE", merchantId))
                    .willReturn(Optional.empty());
            given(productRepository.findByCanonicalNameAndMerchantId("produto fantasma", merchantId))
                    .willReturn(Optional.empty());
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            importService.importOrder(detail, OrderStatus.PAID);

            then(notificationService).should()
                    .createMissingProduct(eq("Produto Fantasma"), eq("produto fantasma"), eq(merchantId));
        }

        @Test
        @DisplayName("deve resolver complemento (option) por nome canônico e criar extra ingredient")
        void shouldResolveOptionAsExtraIngredient() {
            IfoodOrderDetailResponse detail = baseDetail();
            IfoodOrderDetailResponse.Option option = new IfoodOrderDetailResponse.Option();
            option.setName("Morango");
            option.setQuantity(BigDecimal.ONE);
            option.setPrice(new BigDecimal("1.50"));
            detail.getItems().get(0).setOptions(List.of(option));

            Ingredient morango = Ingredient.builder()
                    .id(UUID.randomUUID())
                    .name("Morango")
                    .canonicalName("morango")
                    .unit("g")
                    .costPerUnit(new BigDecimal("0.05"))
                    .defaultQuantity(new BigDecimal("30"))
                    .build();

            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(ingredientRepository.findByCanonicalNameAndMerchantId("morango", merchantId))
                    .willReturn(Optional.of(morango));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(detail, OrderStatus.PAID);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getItems().get(0).getExtraIngredients().size() == 1
                            && "Morango".equals(o.getItems().get(0).getExtraIngredients().get(0).getIngredientName())
                            && new BigDecimal("30").compareTo(
                                    o.getItems().get(0).getExtraIngredients().get(0).getQuantity()) == 0));
        }

        @Test
        @DisplayName("deve notificar MISSING_INGREDIENT quando complemento não casa com ingrediente")
        void shouldNotifyMissingIngredientWhenOptionHasNoMatch() {
            IfoodOrderDetailResponse detail = baseDetail();
            IfoodOrderDetailResponse.Option option = new IfoodOrderDetailResponse.Option();
            option.setName("Pistache Raro");
            option.setQuantity(BigDecimal.ONE);
            detail.getItems().get(0).setOptions(List.of(option));

            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(ingredientRepository.findByCanonicalNameAndMerchantId("pistache raro", merchantId))
                    .willReturn(Optional.empty());
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(detail, OrderStatus.PAID);

            assertThat(imported).isTrue();
            then(notificationService).should()
                    .createMissingIngredient(eq("Pistache Raro"), eq("pistache raro"), eq(merchantId));
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getItems().get(0).getExtraIngredients().isEmpty()));
        }

        @Test
        @DisplayName("deve converter createdAt UTC para horário de Brasília")
        void shouldConvertCreatedAtToBrazilZone() {
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            importService.importOrder(baseDetail(), OrderStatus.PAID);

            // 2026-07-01T18:00:00Z == 15:00 em America/Sao_Paulo (UTC-3)
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getDateTime().getHour() == 15 && o.getDateTime().getDayOfMonth() == 1));
        }

        @Test
        @DisplayName("deve reutilizar cliente existente pelo telefone")
        void shouldReuseExistingCustomerByPhone() {
            Customer existing = Customer.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .name("Maria Santos")
                    .phone("11999998888")
                    .build();
            given(customerRepository.findByPhoneAndMerchantId("11999998888", merchantId))
                    .willReturn(Optional.of(existing));
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            importService.importOrder(baseDetail(), OrderStatus.PAID);

            then(customerRepository).should(never()).save(any(Customer.class));
            then(orderRepository).should().save(argThat((Order o) -> o.getCustomer() == existing));
        }

        @Test
        @DisplayName("deve reutilizar cliente existente pelo externalId do iFood antes do telefone")
        void shouldReuseExistingCustomerByExternalIdFirst() {
            IfoodOrderDetailResponse detail = baseDetail();
            detail.getCustomer().setId("ifood-cust-1");

            Customer existing = Customer.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .name("Maria Santos")
                    .externalId("ifood-cust-1")
                    .build();
            given(customerRepository.findByExternalIdAndMerchantId("ifood-cust-1", merchantId))
                    .willReturn(Optional.of(existing));
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            importService.importOrder(detail, OrderStatus.PAID);

            then(customerRepository).should(never()).findByPhoneAndMerchantId(anyString(), any());
            then(customerRepository).should(never()).save(any(Customer.class));
            then(orderRepository).should().save(argThat((Order o) -> o.getCustomer() == existing));
        }

        @Test
        @DisplayName("não deve deduplicar nem persistir telefone 0800 (proxy do iFood)")
        void shouldNotDedupOrPersistIfoodProxyPhone() {
            IfoodOrderDetailResponse detail = baseDetail();
            detail.getCustomer().setId("ifood-cust-2");
            detail.getCustomer().getPhone().setNumber("0800 700 3020");

            given(customerRepository.findByExternalIdAndMerchantId("ifood-cust-2", merchantId))
                    .willReturn(Optional.empty());
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            importService.importOrder(detail, OrderStatus.PAID);

            then(customerRepository).should(never()).findByPhoneAndMerchantId(anyString(), any());
            then(customerRepository).should().save(argThat((Customer c) ->
                    c.getPhone() == null
                            && "ifood-cust-2".equals(c.getExternalId())
                            && "Maria Santos".equals(c.getName())));
        }

        @Test
        @DisplayName("deve importar com status PENDING quando o evento de origem é CONFIRMED")
        void shouldImportWithPendingStatus() {
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(baseDetail(), OrderStatus.PENDING);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getStatus() == OrderStatus.PENDING));
        }

        @Test
        @DisplayName("deve importar com status CANCELLED quando o evento de origem é CANCELLED")
        void shouldImportWithCancelledStatus() {
            given(productRepository.findByExternalIdAndMerchantId("PDV-1", merchantId))
                    .willReturn(Optional.of(product));
            given(orderRepository.existsByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(false);

            boolean imported = importService.importOrder(baseDetail(), OrderStatus.CANCELLED);

            assertThat(imported).isTrue();
            then(orderRepository).should().save(argThat((Order o) ->
                    o.getStatus() == OrderStatus.CANCELLED));
        }
    }

    private Order existingOrder(OrderStatus status) {
        return Order.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .externalOrderId("ord-1")
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("concludeOrder()")
    class ConcludeOrder {

        @Test
        @DisplayName("deve atualizar pedido PENDING existente para PAID e retornar true")
        void shouldUpdateExistingPendingOrderToPaid() {
            Order order = existingOrder(OrderStatus.PENDING);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.concludeOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            then(orderRepository).should().save(order);
        }

        @Test
        @DisplayName("não deve reverter pedido CANCELLED (CANCELLED sempre vence)")
        void shouldNotRevertCancelledOrder() {
            Order order = existingOrder(OrderStatus.CANCELLED);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.concludeOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("não deve promover pedido TEST para PAID (teste fica fora dos ganhos)")
        void shouldNotPromoteTestOrder() {
            Order order = existingOrder(OrderStatus.TEST);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.concludeOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.TEST);
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve ser idempotente para pedido já PAID (não salva de novo)")
        void shouldBeIdempotentForPaidOrder() {
            Order order = existingOrder(OrderStatus.PAID);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.concludeOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve retornar false quando o pedido não existe (aciona o import completo)")
        void shouldReturnFalseWhenOrderUnknown() {
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.empty());

            boolean handled = importService.concludeOrder("ord-1", "ifood-m1");

            assertThat(handled).isFalse();
            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve retornar false quando o merchant iFood é desconhecido")
        void shouldReturnFalseWhenMerchantUnknown() {
            given(merchantRepository.findByIfoodMerchantId("ifood-m1")).willReturn(Optional.empty());

            boolean handled = importService.concludeOrder("ord-1", "ifood-m1");

            assertThat(handled).isFalse();
            then(orderRepository).should(never()).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("cancelOrder()")
    class CancelOrder {

        @Test
        @DisplayName("deve cancelar pedido PENDING existente e notificar ORDER_CANCELLED")
        void shouldCancelExistingPendingOrderAndNotify() {
            Order order = existingOrder(OrderStatus.PENDING);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.cancelOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(orderRepository).should().save(order);
            then(notificationService).should().createOrderCancelled("ord-1", null, merchantId);
        }

        @Test
        @DisplayName("deve cancelar pedido já PAID (CANCELLED vence sobre PAID)")
        void shouldCancelPaidOrder() {
            Order order = existingOrder(OrderStatus.PAID);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.cancelOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(orderRepository).should().save(order);
            then(notificationService).should().createOrderCancelled("ord-1", null, merchantId);
        }

        @Test
        @DisplayName("não deve cancelar nem notificar pedido TEST (status TEST é terminal)")
        void shouldNotCancelTestOrder() {
            Order order = existingOrder(OrderStatus.TEST);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.cancelOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.TEST);
            then(orderRepository).should(never()).save(any(Order.class));
            then(notificationService).should(never()).createOrderCancelled(anyString(), any(), any());
        }

        @Test
        @DisplayName("deve ser idempotente: pedido já CANCELLED não salva nem notifica de novo")
        void shouldBeIdempotentForCancelledOrder() {
            Order order = existingOrder(OrderStatus.CANCELLED);
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.of(order));

            boolean handled = importService.cancelOrder("ord-1", "ifood-m1");

            assertThat(handled).isTrue();
            then(orderRepository).should(never()).save(any(Order.class));
            then(notificationService).should(never()).createOrderCancelled(anyString(), any(), any());
        }

        @Test
        @DisplayName("deve retornar false sem notificar quando o pedido não existe")
        void shouldReturnFalseWhenOrderUnknown() {
            given(orderRepository.findByExternalOrderIdAndMerchantId("ord-1", merchantId))
                    .willReturn(Optional.empty());

            boolean handled = importService.cancelOrder("ord-1", "ifood-m1");

            assertThat(handled).isFalse();
            then(notificationService).should(never()).createOrderCancelled(anyString(), any(), any());
        }

        @Test
        @DisplayName("deve retornar false quando o merchant iFood é desconhecido")
        void shouldReturnFalseWhenMerchantUnknown() {
            given(merchantRepository.findByIfoodMerchantId("ifood-m1")).willReturn(Optional.empty());

            boolean handled = importService.cancelOrder("ord-1", "ifood-m1");

            assertThat(handled).isFalse();
            then(orderRepository).should(never()).save(any(Order.class));
        }
    }
}
