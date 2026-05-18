package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.payment.PaymentMethod;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.Product;
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

    @Mock
    private AnotaAIClient anotaAIClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private OrderRepository orderRepository;

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
    }

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
                .willAnswer(inv -> {
                    Category c = inv.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });

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
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Old Name")
                .externalId("cat-1")
                .build();
        Product existingProduct = Product.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Old Product")
                .price(new BigDecimal("5.00"))
                .status(ProductStatus.ACTIVE)
                .externalId("item-1")
                .build();

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
    @DisplayName("syncCatalog deve ignorar categorias com is_additional = true")
    void syncCatalog_shouldSkipAdditionalCategories() {
        AnotaAICatalogResponse catalog = new AnotaAICatalogResponse();
        AnotaAICatalogResponse.AnotaAICategory additional = new AnotaAICatalogResponse.AnotaAICategory();
        additional.setId("cat-extra");
        additional.setTitle("Adicionais");
        additional.setAdditional(true);
        AnotaAICatalogResponse.AnotaAIItem item = new AnotaAICatalogResponse.AnotaAIItem();
        item.setId("extra-1");
        item.setTitle("Bacon");
        item.setPrice(2.0);
        additional.setItens(List.of(item));
        catalog.setCategories(List.of(additional));

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getCatalog("test-api-key")).willReturn(catalog);

        AnotaAISyncResult result = syncService.syncCatalog(ownerId);

        assertThat(result.getCategoriesCreated()).isZero();
        assertThat(result.getProductsCreated()).isZero();
        verify(categoryRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncOrders deve importar pedidos novos")
    void syncOrders_shouldImportNewOrders() {
        Customer existingCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Teste")
                .phone("43123456789")
                .build();
        Product mappedProduct = Product.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Refrigerante 1L")
                .price(new BigDecimal("10.00"))
                .estimatedCost(BigDecimal.ZERO)
                .status(ProductStatus.ACTIVE)
                .externalId("65d4a428f784bb001956f919")
                .build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId))
                .willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-1"))
                .willReturn(buildOrderDetail("order-1"));
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
    @DisplayName("syncOrders deve pular pedidos já importados")
    void syncOrders_shouldSkipAlreadyImportedOrders() {
        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId))
                .willReturn(true);

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
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Refrigerante 1L")
                .price(new BigDecimal("10.00"))
                .estimatedCost(BigDecimal.ZERO)
                .status(ProductStatus.ACTIVE)
                .externalId("65d4a428f784bb001956f919")
                .build();

        given(userRepository.findById(ownerId)).willReturn(Optional.of(user));
        given(anotaAIClient.getOrderList("test-api-key")).willReturn(buildOrderList("order-1"));
        given(orderRepository.existsByExternalOrderIdAndOwnerId("order-1", ownerId))
                .willReturn(false);
        given(anotaAIClient.getOrderDetail("test-api-key", "order-1"))
                .willReturn(buildOrderDetail("order-1"));
        given(customerRepository.findByPhoneAndOwnerId("43123456789", ownerId))
                .willReturn(Optional.empty());
        given(customerRepository.save(any(Customer.class)))
                .willAnswer(inv -> {
                    Customer c = inv.getArgument(0);
                    c.setId(UUID.randomUUID());
                    return c;
                });
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

    // ----- helpers -----

    private AnotaAICatalogResponse buildCatalog() {
        AnotaAICatalogResponse catalog = new AnotaAICatalogResponse();
        AnotaAICatalogResponse.AnotaAICategory category = new AnotaAICatalogResponse.AnotaAICategory();
        category.setId("cat-1");
        category.setTitle("Bebidas");
        category.setAdditional(false);

        AnotaAICatalogResponse.AnotaAIItem item1 = new AnotaAICatalogResponse.AnotaAIItem();
        item1.setId("item-1");
        item1.setTitle("Refrigerante 1L");
        item1.setPrice(10.0);
        item1.setOut(false);

        AnotaAICatalogResponse.AnotaAIItem item2 = new AnotaAICatalogResponse.AnotaAIItem();
        item2.setId("item-2");
        item2.setTitle("Suco 500ml");
        item2.setPrice(7.0);
        item2.setOut(false);

        category.setItens(List.of(item1, item2));
        catalog.setCategories(List.of(category));
        return catalog;
    }

    private AnotaAIOrderListResponse buildOrderList(String orderId) {
        AnotaAIOrderListResponse response = new AnotaAIOrderListResponse();
        AnotaAIOrderListResponse.OrderListInfo info = new AnotaAIOrderListResponse.OrderListInfo();
        AnotaAIOrderListResponse.OrderSummary summary = new AnotaAIOrderListResponse.OrderSummary();
        summary.setId(orderId);
        summary.setCheck(1);
        summary.setSalesChannel("anotaai");
        info.setDocs(List.of(summary));
        info.setCount(1);
        response.setInfo(info);
        return response;
    }

    private AnotaAIOrderDetailResponse buildOrderDetail(String orderId) {
        AnotaAIOrderDetailResponse response = new AnotaAIOrderDetailResponse();
        AnotaAIOrderDetailResponse.OrderDetail detail = new AnotaAIOrderDetailResponse.OrderDetail();
        detail.setId(orderId);

        AnotaAIOrderDetailResponse.AnotaAICustomer customer = new AnotaAIOrderDetailResponse.AnotaAICustomer();
        customer.setId("cust-1");
        customer.setName("Teste");
        customer.setPhone("43123456789");
        detail.setCustomer(customer);

        AnotaAIOrderDetailResponse.AnotaAIOrderItem item = new AnotaAIOrderDetailResponse.AnotaAIOrderItem();
        item.setName("Refrigerante 1L");
        item.setQuantity(1);
        item.setInternalId("65d4a428f784bb001956f919");
        item.setPrice(10.0);
        item.setTotal(10.0);
        detail.setItems(List.of(item));

        AnotaAIOrderDetailResponse.AnotaAIPayment payment = new AnotaAIOrderDetailResponse.AnotaAIPayment();
        payment.setName("money");
        payment.setCode("money");
        payment.setValue("10");
        detail.setPayments(List.of(payment));

        detail.setTotal(10.0);
        detail.setType("LOCAL");
        response.setInfo(detail);
        return response;
    }
}
