package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductIngredient;
import com.MenuBank.MenuBank.product.ProductIngredientRepository;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import com.MenuBank.MenuBank.user.User;
import com.MenuBank.MenuBank.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnotaAISyncService")
class AnotaAISyncServiceTest {

    @Mock private AnotaAIClient anotaAIClient;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private IngredientRepository ingredientRepository;
    @Mock private ProductIngredientRepository productIngredientRepository;
    @Mock private NotificationService notificationService;
    @Mock private com.MenuBank.MenuBank.product.OrderCostCalculatorService orderCostCalculatorService;

    @InjectMocks
    private AnotaAISyncService syncService;

    private UUID ownerId;
    private User user;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        user = User.builder()
                .id(ownerId)
                .anotaAiApiKey("test-api-key")
                .build();
        // Default: cálculo de custo retorna ZERO — testes que verificam profit/cost específico sobrescrevem
        org.mockito.Mockito.lenient()
                .when(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                .thenReturn(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // syncCatalog — apenas categorias e produtos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("syncCatalog deve criar categorias e produtos novos")
    void syncCatalog_shouldCreateCategoriesAndProducts() {
        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(buildCatalog());
        given(categoryRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(categoryRepository.save(any(Category.class)))
                .willAnswer(inv -> { Category c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c; });

        AnotaAISyncResult result = syncService.syncCatalog(ownerId);

        assertThat(result.getCategoriesCreated()).isEqualTo(1);
        assertThat(result.getCategoriesUpdated()).isZero();
        assertThat(result.getProductsCreated()).isEqualTo(2);
        assertThat(result.getProductsUpdated()).isZero();
        verify(categoryRepository, times(1)).save(any(Category.class));
        verify(productRepository, times(2)).save(any(Product.class));
    }

    @Test
    @DisplayName("syncCatalog deve atualizar produtos existentes")
    void syncCatalog_shouldUpdateExistingProducts() {
        Category existingCategory = Category.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Old Name").externalId("cat-1").build();
        Product existingProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Old Product")
                .price(new BigDecimal("5.00")).status(ProductStatus.ACTIVE).externalId("item-1").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(buildCatalog());
        given(categoryRepository.findByExternalIdAndOwnerId("cat-1", ownerId))
                .willReturn(Optional.of(existingCategory));
        given(productRepository.findByExternalIdAndOwnerId("item-1", ownerId))
                .willReturn(Optional.of(existingProduct));
        given(productRepository.findByExternalIdAndOwnerId("item-2", ownerId))
                .willReturn(Optional.empty());

        AnotaAISyncResult result = syncService.syncCatalog(ownerId);

        assertThat(result.getCategoriesUpdated()).isEqualTo(1);
        assertThat(result.getCategoriesCreated()).isZero();
        assertThat(result.getProductsUpdated()).isEqualTo(1);
        assertThat(result.getProductsCreated()).isEqualTo(1);
        assertThat(existingCategory.getName()).isEqualTo("Bebidas");
        assertThat(existingProduct.getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("syncCatalog NÃO deve tocar em ingredientes — usuário cadastra manualmente")
    void syncCatalog_shouldNotTouchIngredients() {
        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(buildCatalogWithAdditionals());
        given(categoryRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(categoryRepository.save(any(Category.class)))
                .willAnswer(inv -> { Category c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c; });

        syncService.syncCatalog(ownerId);

        // Nada de ingredientes ou categorias de ingrediente é criado no sync de cardápio
        verify(ingredientRepository, never()).save(any(Ingredient.class));
        // Categorias is_additional=true são puladas no Pass único; só cat-1 (is_additional=false) gera Category
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("syncCatalog(ownerId, true) deve apagar ProductIngredients dos produtos vindos do Anota.AI antes de re-importar")
    void syncCatalog_withClearRecipes_shouldDeleteExistingProductIngredients() {
        Product existingProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Old Product")
                .price(new BigDecimal("5.00")).status(ProductStatus.ACTIVE).externalId("item-1").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(buildCatalog());
        given(categoryRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("item-1", ownerId))
                .willReturn(Optional.of(existingProduct));
        given(productRepository.findByExternalIdAndOwnerId("item-2", ownerId))
                .willReturn(Optional.empty());
        given(categoryRepository.save(any(Category.class)))
                .willAnswer(inv -> { Category c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c; });
        given(productRepository.save(any(Product.class)))
                .willAnswer(inv -> { Product p = inv.getArgument(0); if (p.getId() == null) p.setId(UUID.randomUUID()); return p; });

        syncService.syncCatalog(ownerId, true);

        verify(productIngredientRepository).deleteAllByProductIdAndProductOwnerId(existingProduct.getId(), ownerId);
    }

    @Test
    @DisplayName("syncCatalog(ownerId, false) NÃO deve apagar ProductIngredients existentes")
    void syncCatalog_withoutClearRecipes_shouldNotDeleteProductIngredients() {
        Product existingProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Old Product")
                .price(new BigDecimal("5.00")).status(ProductStatus.ACTIVE).externalId("item-1").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(buildCatalog());
        given(categoryRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("item-1", ownerId))
                .willReturn(Optional.of(existingProduct));
        given(productRepository.findByExternalIdAndOwnerId("item-2", ownerId))
                .willReturn(Optional.empty());
        given(categoryRepository.save(any(Category.class)))
                .willAnswer(inv -> { Category c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c; });
        given(productRepository.save(any(Product.class)))
                .willAnswer(inv -> { Product p = inv.getArgument(0); if (p.getId() == null) p.setId(UUID.randomUUID()); return p; });

        syncService.syncCatalog(ownerId, false);

        verify(productIngredientRepository, never()).deleteAllByProductIdAndProductOwnerId(any(), any());
    }

    @Test
    @DisplayName("syncCatalog(ownerId) — overload legado equivale a clearRecipes=false")
    void syncCatalog_legacyOverload_shouldNotClearRecipes() {
        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(buildCatalog());
        given(categoryRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId(anyString(), eq(ownerId)))
                .willReturn(Optional.empty());
        given(categoryRepository.save(any(Category.class)))
                .willAnswer(inv -> { Category c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c; });

        syncService.syncCatalog(ownerId);

        verify(productIngredientRepository, never()).deleteAllByProductIdAndProductOwnerId(any(), any());
    }

    // -------------------------------------------------------------------------
    // syncOrders
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("syncOrders deve importar pedidos novos")
    void syncOrders_shouldImportNewOrders() {
        Customer existingCustomer = Customer.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Teste").phone("43123456789").build();
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Refrigerante 1L")
                .status(ProductStatus.ACTIVE).externalId("65d4a428f784bb001956f919").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-1")).willReturn(buildOrderDetail("order-1"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(existingCustomer));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("65d4a428f784bb001956f919", ownerId))
                .willReturn(Optional.of(mappedProduct));

        AnotaAISyncResult result = syncService.syncOrders(ownerId);

        assertThat(result.getOrdersImported()).isEqualTo(1);
        assertThat(result.getOrdersSkipped()).isZero();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getOrigin()).isEqualTo(OrderOrigin.ANOTA_AI);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getExternalOrderId()).isEqualTo("order-1");
        assertThat(saved.getCustomer()).isEqualTo(existingCustomer);
        assertThat(saved.getTotalValue()).isEqualByComparingTo("10.00");
        assertThat(saved.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("syncOrders deve calcular estimatedProfit como sum(item.profit) — sem subtrair fees")
    void syncOrders_shouldComputeEstimatedProfitWithoutPaymentMethod() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Refrigerante 1L")
                .status(ProductStatus.ACTIVE).externalId("65d4a428f784bb001956f919").build();
        Ingredient costIng = Ingredient.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Cost").unit("un")
                .costPerUnit(new BigDecimal("4.00")).status(IngredientStatus.ACTIVE).build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-1")).willReturn(buildOrderDetail("order-1"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("65d4a428f784bb001956f919", ownerId))
                .willReturn(Optional.of(mappedProduct));
        given(productIngredientRepository.findByProductIdAndProductOwnerId(mappedProduct.getId(), ownerId))
                .willReturn(List.of(ProductIngredient.builder().product(mappedProduct).ingredient(costIng)
                        .grammage(BigDecimal.ONE).build()));
        given(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                .willReturn(new BigDecimal("4.00"));

        syncService.syncOrders(ownerId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getEstimatedProfit()).isEqualByComparingTo("6.00");
        assertThat(orderCaptor.getValue().getTotalCost()).isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("syncOrders deve reclassificar origin de pedido já existente quando salesChannel mudou")
    void syncOrders_shouldReclassifyOriginOfExistingOrderWhenSalesChannelDiffers() {
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .externalOrderId("order-ifood")
                .origin(OrderOrigin.ANOTA_AI)
                .build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key"))
                .willReturn(buildOrderListWithSalesChannel("order-ifood", "ifood"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-ifood", ownerId)).willReturn(true);
        given(orderRepository.findByExternalOrderIdAndOwnerId("order-ifood", ownerId))
                .willReturn(Optional.of(existingOrder));

        AnotaAISyncResult result = syncService.syncOrders(ownerId);

        assertThat(existingOrder.getOrigin()).isEqualTo(OrderOrigin.IFOOD);
        verify(orderRepository).save(existingOrder);
        verify(anotaAIClient, never()).getOrderDetail(anyString(), eq("order-ifood"));
        assertThat(result.getOrdersImported()).isZero();
        assertThat(result.getOrdersSkipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("syncOrders NÃO deve salvar pedido existente quando origin já está correta")
    void syncOrders_shouldNotSaveExistingOrderWhenOriginIsAlreadyCorrect() {
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .externalOrderId("order-anota")
                .origin(OrderOrigin.ANOTA_AI)
                .build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key"))
                .willReturn(buildOrderListWithSalesChannel("order-anota", "anotaai"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-anota", ownerId)).willReturn(true);
        given(orderRepository.findByExternalOrderIdAndOwnerId("order-anota", ownerId))
                .willReturn(Optional.of(existingOrder));

        syncService.syncOrders(ownerId);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("syncOrders deve marcar origin=IFOOD quando salesChannel=ifood no /ping/list")
    void syncOrders_shouldMarkOriginAsIfoodWhenSalesChannelIsIfood() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Refrigerante 1L")
                .status(ProductStatus.ACTIVE).externalId("65d4a428f784bb001956f919").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key"))
                .willReturn(buildOrderListWithSalesChannel("order-ifood", "ifood"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-ifood", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-ifood"))
                .willReturn(buildOrderDetail("order-ifood"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(productRepository.findByExternalIdAndOwnerId("65d4a428f784bb001956f919", ownerId))
                .willReturn(Optional.of(mappedProduct));

        syncService.syncOrders(ownerId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrigin()).isEqualTo(OrderOrigin.IFOOD);
    }

    @Test
    @DisplayName("syncOrders deve manter origin=ANOTA_AI para outros salesChannel")
    void syncOrders_shouldDefaultToAnotaAiOriginForNonIfoodChannels() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Refrigerante 1L")
                .status(ProductStatus.ACTIVE).externalId("65d4a428f784bb001956f919").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key"))
                .willReturn(buildOrderListWithSalesChannel("order-whats", "anotaai"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-whats", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-whats"))
                .willReturn(buildOrderDetail("order-whats"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(productRepository.findByExternalIdAndOwnerId("65d4a428f784bb001956f919", ownerId))
                .willReturn(Optional.of(mappedProduct));

        syncService.syncOrders(ownerId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrigin()).isEqualTo(OrderOrigin.ANOTA_AI);
    }

    @Test
    @DisplayName("importOrder deve persistir deliveryFee e descontá-la do lucro estimado")
    void importOrder_shouldPersistDeliveryFeeAndUseItInProfit() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Açaí 500ml")
                .status(ProductStatus.ACTIVE).externalId("65d4a428f784bb001956f919").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-1"))
                .willReturn(buildOrderDetailWithDeliveryFee("order-1", 25.80, 6.00));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("65d4a428f784bb001956f919", ownerId))
                .willReturn(Optional.of(mappedProduct));
        given(productIngredientRepository.findByProductIdAndProductOwnerId(mappedProduct.getId(), ownerId))
                .willReturn(List.of());
        given(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                .willReturn(BigDecimal.ZERO);

        syncService.syncOrders(ownerId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getDeliveryFee()).isEqualByComparingTo("6.00");
        assertThat(saved.getTotalValue()).isEqualByComparingTo("25.80");
        // estimatedProfit = totalValue (25.80) − deliveryFee (6.00) − totalCost (0) = 19.80
        assertThat(saved.getEstimatedProfit()).isEqualByComparingTo("19.80");
        assertThat(saved.getTotalCost()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("syncOrders deve pular pedidos já importados")
    void syncOrders_shouldSkipAlreadyImportedOrders() {
        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId)).willReturn(true);

        AnotaAISyncResult result = syncService.syncOrders(ownerId);

        assertThat(result.getOrdersImported()).isZero();
        assertThat(result.getOrdersSkipped()).isEqualTo(1);
        verify(anotaAIClient, never()).getOrderDetail(anyString(), anyString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncOrders deve criar cliente novo se não existir")
    void syncOrders_shouldCreateCustomerIfNotFound() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Refrigerante 1L")
                .status(ProductStatus.ACTIVE).externalId("65d4a428f784bb001956f919").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-1")).willReturn(buildOrderDetail("order-1"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId)).willReturn(Optional.empty());
        given(customerRepository.save(any(Customer.class)))
                .willAnswer(inv -> { Customer c = inv.getArgument(0); c.setId(UUID.randomUUID()); return c; });
        given(productRepository.findByExternalIdAndOwnerId("65d4a428f784bb001956f919", ownerId))
                .willReturn(Optional.of(mappedProduct));

        AnotaAISyncResult result = syncService.syncOrders(ownerId);

        assertThat(result.getOrdersImported()).isEqualTo(1);
        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer created = customerCaptor.getValue();
        assertThat(created.getName()).isEqualTo("Teste");
        assertThat(created.getPhone()).isEqualTo("43123456789");
        assertThat(created.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("syncOrders deve lançar exceção se usuário não tiver API key")
    void syncOrders_shouldFailIfNoApiKey() {
        User userWithoutKey = User.builder().id(ownerId).anotaAiApiKey(null).build();
        given(userRepository.findById(ownerId)).willReturn(Optional.of(userWithoutKey));

        assertThatThrownBy(() -> syncService.syncOrders(ownerId))
                .isInstanceOf(AnotaAIIntegrationException.class);
    }

    // -------------------------------------------------------------------------
    // syncOrders — match de extras por nome canônico (refactor central)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("syncOrders deve resolver extra ingredient por canonical name (normalizado)")
    void syncOrders_shouldMatchExtraIngredientByCanonicalName() {
        Ingredient acaiPremium = Ingredient.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Açaí Premium").unit("ml")
                .costPerUnit(new BigDecimal("0.05")).defaultQuantity(new BigDecimal("500"))
                .status(IngredientStatus.ACTIVE).build();
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Açaí 500ml")
                .status(ProductStatus.ACTIVE).externalId("product-internal-id").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-2"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-2", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-2"))
                .willReturn(buildOrderDetailWithSubItems("order-2"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("product-internal-id", ownerId))
                .willReturn(Optional.of(mappedProduct));
        // subItem.name = "Açaí Premium" → canonical "acai premium"
        given(ingredientRepository.findByCanonicalNameAndOwnerId("acai premium", ownerId))
                .willReturn(Optional.of(acaiPremium));

        syncService.syncOrders(ownerId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getExtraIngredients()).hasSize(1);
        var extra = saved.getItems().get(0).getExtraIngredients().get(0);
        assertThat(extra.getIngredient()).isEqualTo(acaiPremium);
        // quantity = subItem.quantity (1) × ingredient.defaultQuantity (500) = 500
        assertThat(extra.getQuantity()).isEqualByComparingTo("500");
        assertThat(extra.getCostPerUnit()).isEqualByComparingTo("0.05");
        assertThat(extra.getIngredientName()).isEqualTo("Açaí Premium");
        assertThat(extra.getIngredientUnit()).isEqualTo("ml");
        // Nenhuma notificação foi criada — ingrediente foi encontrado
        verify(notificationService, never()).createMissingIngredient(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("syncOrders deve respeitar subItem.quantity ao montar OrderItemExtraIngredient")
    void syncOrders_shouldRespectSubItemQuantityForExtra() {
        Ingredient leiteNinho = Ingredient.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("leite ninho").unit("g")
                .costPerUnit(new BigDecimal("0.0533"))
                .defaultQuantity(new BigDecimal("20"))
                .status(IngredientStatus.ACTIVE).build();
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Açaí 330ml")
                .status(ProductStatus.ACTIVE).externalId("product-internal-id").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-q2"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-q2", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-q2"))
                .willReturn(AnotaAIFixtures.load("order_detail_with_subitem_quantity_two.json",
                        AnotaAIOrderDetailResponse.class));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("product-internal-id", ownerId))
                .willReturn(Optional.of(mappedProduct));
        // subItem.name = "leite ninho" → canonical "leite ninho"
        given(ingredientRepository.findByCanonicalNameAndOwnerId("leite ninho", ownerId))
                .willReturn(Optional.of(leiteNinho));

        syncService.syncOrders(ownerId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        var extra = orderCaptor.getValue().getItems().get(0).getExtraIngredients().get(0);
        // quantity = subItem.quantity (2) × ingredient.defaultQuantity (20) = 40 g
        assertThat(extra.getQuantity()).isEqualByComparingTo("40");
        assertThat(extra.getCostPerUnit()).isEqualByComparingTo("0.0533");
    }

    @Test
    @DisplayName("syncOrders deve criar notificação e pular extra quando ingrediente não está cadastrado")
    void syncOrders_shouldCreateMissingIngredientNotificationWhenNotFound() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Açaí 500ml")
                .status(ProductStatus.ACTIVE).externalId("product-internal-id").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-3"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-3", ownerId)).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-3"))
                .willReturn(buildOrderDetailWithSubItems("order-3"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("product-internal-id", ownerId))
                .willReturn(Optional.of(mappedProduct));
        // Ingrediente "Açaí Premium" não encontrado
        given(ingredientRepository.findByCanonicalNameAndOwnerId("acai premium", ownerId))
                .willReturn(Optional.empty());

        AnotaAISyncResult result = syncService.syncOrders(ownerId);

        // Pedido foi importado, mas sem o extra
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getItems().get(0).getExtraIngredients()).isEmpty();

        // Notificação criada
        verify(notificationService).createMissingIngredient("Açaí Premium", "acai premium", ownerId);
        // Nome aparece no resultado
        assertThat(result.getMissingIngredientNames()).containsExactly("Açaí Premium");
    }

    @Test
    @DisplayName("syncOrders deve ouvinte notificar uma única vez para o mesmo nome quando aparece em múltiplos pedidos do mesmo sync")
    void syncOrders_shouldReportDistinctMissingNamesAcrossOrders() {
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("Açaí 500ml")
                .status(ProductStatus.ACTIVE).externalId("product-internal-id").build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        // 2 pedidos com o mesmo subItem "Açaí Premium" faltando
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderListWith2Orders("ord-A", "ord-B"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId(anyString(), eq(ownerId))).willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "ord-A"))
                .willReturn(buildOrderDetailWithSubItems("ord-A"));
        given(anotaAIClient.getOrderDetail("test-api-key", "ord-B"))
                .willReturn(buildOrderDetailWithSubItems("ord-B"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.of(Customer.builder().id(UUID.randomUUID()).ownerId(ownerId).build()));
        given(paymentMethodRepository.findByNameIgnoreCaseAndOwnerId("money", ownerId))
                .willReturn(Optional.empty());
        given(productRepository.findByExternalIdAndOwnerId("product-internal-id", ownerId))
                .willReturn(Optional.of(mappedProduct));
        given(ingredientRepository.findByCanonicalNameAndOwnerId("acai premium", ownerId))
                .willReturn(Optional.empty());

        AnotaAISyncResult result = syncService.syncOrders(ownerId);

        // Nome único no resultado (não duplica)
        assertThat(result.getMissingIngredientNames()).containsExactly("Açaí Premium");
        // Notificação foi chamada (a dedupe acontece no NotificationService, então pode ser chamada 2x)
        verify(notificationService, times(2))
                .createMissingIngredient("Açaí Premium", "acai premium", ownerId);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private AnotaAICatalogResponse buildCatalog() {
        return AnotaAIFixtures.load("catalog_minimal.json", AnotaAICatalogResponse.class);
    }

    private AnotaAICatalogResponse buildCatalogWithAdditionals() {
        return AnotaAIFixtures.load("catalog_with_additionals.json", AnotaAICatalogResponse.class);
    }

    private AnotaAIOrderListResponse buildOrderList(String orderId) {
        return buildOrderListWithSalesChannel(orderId, "anotaai");
    }

    private AnotaAIOrderListResponse buildOrderListWithSalesChannel(String orderId, String salesChannel) {
        AnotaAIOrderListResponse response = AnotaAIFixtures.load("order_list_template.json",
                AnotaAIOrderListResponse.class);
        AnotaAIOrderListResponse.OrderSummary summary = response.getInfo().getDocs().get(0);
        summary.setId(orderId);
        summary.setSalesChannel(salesChannel);
        return response;
    }

    private AnotaAIOrderListResponse buildOrderListWith2Orders(String idA, String idB) {
        AnotaAIOrderListResponse response = AnotaAIFixtures.load("order_list_template.json",
                AnotaAIOrderListResponse.class);
        AnotaAIOrderListResponse.OrderSummary first = response.getInfo().getDocs().get(0);
        first.setId(idA);
        first.setSalesChannel("anotaai");
        AnotaAIOrderListResponse.OrderSummary second = new AnotaAIOrderListResponse.OrderSummary();
        second.setId(idB);
        second.setSalesChannel("anotaai");
        second.setFrom(first.getFrom());
        second.setUpdatedAt(first.getUpdatedAt());
        response.getInfo().getDocs().add(second);
        return response;
    }

    private AnotaAIOrderDetailResponse buildOrderDetailWithSubItems(String orderId) {
        AnotaAIOrderDetailResponse response = AnotaAIFixtures.load("order_detail_with_subitems.json",
                AnotaAIOrderDetailResponse.class);
        response.getInfo().setId(orderId);
        return response;
    }

    private AnotaAIOrderDetailResponse buildOrderDetailWithDeliveryFee(String orderId, double total, double deliveryFee) {
        AnotaAIOrderDetailResponse response = buildOrderDetail(orderId);
        response.getInfo().setTotal(total);
        response.getInfo().setDeliveryFee(deliveryFee);
        return response;
    }

    private AnotaAIOrderDetailResponse buildOrderDetail(String orderId) {
        AnotaAIOrderDetailResponse response = AnotaAIFixtures.load("order_detail_simple.json",
                AnotaAIOrderDetailResponse.class);
        response.getInfo().setId(orderId);
        return response;
    }
}
