package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import com.MenuBank.MenuBank.product.RecipeItem;
import com.MenuBank.MenuBank.product.RecipeItemRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private RecipeItemRepository recipeItemRepository;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private UUID ownerId;
    private UUID customerId;
    private UUID productId;
    private UUID ingredientId;
    private Customer customer;
    private Product product;
    private Ingredient ingredient;
    private Order order;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();

        customer = Customer.builder()
                .id(customerId)
                .ownerId(ownerId)
                .name("Cliente Teste")
                .phone("11999999999")
                .email("cliente@email.com")
                .build();

        product = Product.builder()
                .id(productId)
                .ownerId(ownerId)
                .name("Hambúrguer")
                .price(new BigDecimal("30.00"))
                .status(ProductStatus.ACTIVE)
                .build();

        ingredient = Ingredient.builder()
                .id(ingredientId)
                .ownerId(ownerId)
                .name("Bacon")
                .unit("g")
                .costPerUnit(new BigDecimal("0.10"))
                .status(IngredientStatus.ACTIVE)
                .build();

        // Mock receita do produto: 1 ingrediente × custo 12.00 = unitCost 12.00
        // (substitui o antigo product.estimatedCost = 12)
        Ingredient costIngredient = Ingredient.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name("CustoBase")
                .unit("un").costPerUnit(new BigDecimal("12.00"))
                .status(IngredientStatus.ACTIVE).build();
        RecipeItem defaultRecipe = RecipeItem.builder()
                .product(product).ingredient(costIngredient)
                .quantity(BigDecimal.ONE).build();
        lenient().when(recipeItemRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .thenReturn(List.of(defaultRecipe));

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(productId)
                .quantity(2)
                .build();

        orderRequest = OrderRequest.builder()
                .customerId(customerId)
                .items(List.of(itemRequest))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("30.00"))
                .unitCost(new BigDecimal("12.00"))
                .build();

        order = Order.builder()
                .id(orderId)
                .ownerId(ownerId)
                .dateTime(LocalDateTime.now())
                .customer(customer)
                .status(OrderStatus.PAID)
                .totalValue(new BigDecimal("60.00"))
                .estimatedProfit(new BigDecimal("36.00"))
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("deve criar pedido com dados válidos e retornar OrderResponse")
        void shouldCreateOrderAndReturnOrderResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            OrderResponse result = orderService.create(orderRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getCustomerName()).isEqualTo("Cliente Teste");
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("deve definir status como PAID (concluído) por padrão")
        void shouldSetStatusToPaidByDefault() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            then(orderRepository).should().save(argThat(savedOrder -> savedOrder.getStatus() == OrderStatus.PAID));
        }

        @Test
        @DisplayName("deve calcular totalValue a partir dos itens (quantidade × preço)")
        void shouldCalculateTotalValueFromItems() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            // 2 × R$30.00 = R$60.00
            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getTotalValue().compareTo(new BigDecimal("60.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve calcular estimatedProfit (totalValue − custo total dos itens)")
        void shouldCalculateEstimatedProfitFromItems() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            // totalValue(60.00) − totalCost(2 × 12.00 = 24.00) = 36.00
            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getEstimatedProfit().compareTo(new BigDecimal("36.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve definir unitPrice do item com o preço atual do produto")
        void shouldSetItemUnitPriceFromProductPrice() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(orderRequest);

            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getItems().get(0).getUnitPrice()
                            .compareTo(new BigDecimal("30.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve definir dateTime no momento da criação")
        void shouldSetDateTimeOnCreation() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            LocalDateTime before = LocalDateTime.now();
            orderService.create(orderRequest);
            LocalDateTime after = LocalDateTime.now();

            then(orderRepository).should().save(argThat(savedOrder ->
                    !savedOrder.getDateTime().isBefore(before) &&
                    !savedOrder.getDateTime().isAfter(after)
            ));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando cliente não encontrado")
        void shouldThrowWhenCustomerNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(orderRequest))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Cliente");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando produto não encontrado")
        void shouldThrowWhenProductNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(orderRequest))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Produto");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve mapear corretamente os itens na resposta")
        void shouldMapItemsCorrectlyInResponse() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            OrderResponse result = orderService.create(orderRequest);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductName()).isEqualTo("Hambúrguer");
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
            assertThat(result.getItems().get(0).getUnitPrice())
                    .isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("deve incluir custo de ingredientes extras no cálculo de estimatedProfit")
        void shouldIncludeExtraIngredientsCostWhenCalculatingEstimatedProfit() {
            OrderItemExtraIngredientRequest extra = OrderItemExtraIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    // quantidade extra por unidade do produto
                    .quantity(new BigDecimal("50"))
                    .build();

            OrderItemRequest itemWithExtra = OrderItemRequest.builder()
                    .productId(productId)
                    .quantity(2)
                    .extraIngredients(List.of(extra))
                    .build();

            OrderRequest requestWithExtra = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(itemWithExtra))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));

            // return a saved copy so OrderResponse uses the calculated values
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(orderId);
                saved.getItems().forEach(i -> i.setId(UUID.randomUUID()));
                return saved;
            });

            OrderResponse response = orderService.create(requestWithExtra);

            // totalValue = 2 * 30.00 = 60.00
            // baseCost = 2 * 12.00 = 24.00
            // extraCost(per unit) = 50 * 0.10 = 5.00; for 2 units => 10.00
            // estimatedProfit = 60.00 - (24.00 + 10.00) = 26.00
            assertThat(response.getEstimatedProfit()).isEqualByComparingTo(new BigDecimal("26.00"));
        }

        @Test
        @DisplayName("deve expor unitCost e totalCost por item na resposta")
        void shouldExposeUnitCostAndTotalCostPerItem() {
            OrderItemExtraIngredientRequest extra = OrderItemExtraIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    .quantity(new BigDecimal("50"))
                    .build();

            OrderItemRequest itemWithExtra = OrderItemRequest.builder()
                    .productId(productId)
                    .quantity(2)
                    .extraIngredients(List.of(extra))
                    .build();

            OrderRequest requestWithExtra = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(itemWithExtra))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(ingredient));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(orderId);
                saved.getItems().forEach(i -> i.setId(UUID.randomUUID()));
                return saved;
            });

            OrderResponse response = orderService.create(requestWithExtra);

            // unitCost = product.estimatedCost (12) + extras (50 * 0.10 = 5) = 17
            // totalCost = unitCost * quantity (2) = 34
            OrderItemResponse itemResponse = response.getItems().get(0);
            assertThat(itemResponse.getUnitCost()).isEqualByComparingTo(new BigDecimal("17.00"));
            assertThat(itemResponse.getTotalCost()).isEqualByComparingTo(new BigDecimal("34.00"));
        }

        @Test
        @DisplayName("deve suportar costPerUnit com 4 casas decimais sem perder precisão")
        void shouldSupportFourDecimalCostPerUnit() {
            Ingredient fineIngredient = Ingredient.builder()
                    .id(ingredientId)
                    .ownerId(ownerId)
                    .name("Açúcar refinado")
                    .unit("g")
                    .costPerUnit(new BigDecimal("0.0035"))
                    .status(IngredientStatus.ACTIVE)
                    .build();

            OrderItemExtraIngredientRequest extra = OrderItemExtraIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    .quantity(new BigDecimal("100"))
                    .build();

            OrderItemRequest itemWithExtra = OrderItemRequest.builder()
                    .productId(productId)
                    .quantity(1)
                    .extraIngredients(List.of(extra))
                    .build();

            OrderRequest requestWithExtra = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(itemWithExtra))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.of(fineIngredient));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(orderId);
                saved.getItems().forEach(i -> i.setId(UUID.randomUUID()));
                return saved;
            });

            OrderResponse response = orderService.create(requestWithExtra);

            // totalValue = 1 * 30.00 = 30.00
            // extraCost = 100 * 0.0035 = 0.3500
            // baseCost = 1 * 12.00 = 12.00
            // estimatedProfit = 30.00 - (12 + 0.35) = 17.65
            assertThat(response.getEstimatedProfit())
                    .isEqualByComparingTo(new BigDecimal("17.6500"));
        }

        @Test
        @DisplayName("deve lançar IngredientNotFoundException quando ingrediente extra não existe")
        void shouldThrowWhenExtraIngredientNotFound() {
            OrderItemExtraIngredientRequest extra = OrderItemExtraIngredientRequest.builder()
                    .ingredientId(ingredientId)
                    .quantity(new BigDecimal("1"))
                    .build();

            OrderItemRequest itemWithExtra = OrderItemRequest.builder()
                    .productId(productId)
                    .quantity(1)
                    .extraIngredients(List.of(extra))
                    .build();

            OrderRequest requestWithExtra = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(itemWithExtra))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndOwnerId(ingredientId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(requestWithExtra))
                    .isInstanceOf(IngredientNotFoundException.class)
                    .hasMessageContaining("Ingrediente");

            then(orderRepository).should(never()).save(any(Order.class));
        }
    }

    // -------------------------------------------------------------------------
    // findById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar OrderResponse quando pedido existe")
        void shouldReturnOrderResponseWhenExists() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(orderId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result.getCustomerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("deve calcular estimatedProfit a partir do unitCost dos itens, ignorando valor armazenado")
        void shouldComputeEstimatedProfitFromItemCosts() {
            OrderItem itemWithZeroCost = OrderItem.builder()
                    .id(UUID.randomUUID())
                    .product(product)
                    .quantity(1)
                    .unitPrice(new BigDecimal("22.49"))
                    .unitCost(BigDecimal.ZERO)
                    .build();
            Order orderWithStaleProfit = Order.builder()
                    .id(orderId)
                    .ownerId(ownerId)
                    .dateTime(LocalDateTime.now())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalValue(new BigDecimal("22.49"))
                    .estimatedProfit(new BigDecimal("-42.01"))
                    .items(new ArrayList<>(List.of(itemWithZeroCost)))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId))
                    .willReturn(Optional.of(orderWithStaleProfit));

            OrderResponse result = orderService.findById(orderId);

            // 22.49 - (unitCost=0 × qty=1) - fee=0 = 22.49
            assertThat(result.getEstimatedProfit()).isEqualByComparingTo(new BigDecimal("22.49"));
        }
    }

    // -------------------------------------------------------------------------
    // findAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAll(search, pageable)")
    class FindAll {

        @Test
        @DisplayName("deve retornar página de pedidos filtrada por nome do cliente (contains, case-insensitive)")
        void shouldReturnPagedOrdersFilteredByCustomerName() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findPageByOwnerIdAndCustomerNameContaining(ownerId, "client", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(order), pageable, 1));

            org.springframework.data.domain.Page<OrderResponse> result =
                    orderService.findAll("client", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(orderId);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve tratar search nulo como string vazia")
        void shouldTreatNullSearchAsEmpty() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findPageByOwnerIdAndCustomerNameContaining(ownerId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));

            org.springframework.data.domain.Page<OrderResponse> result =
                    orderService.findAll(null, pageable);

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
        @DisplayName("deve atualizar pedido existente e retornar OrderResponse atualizado")
        void shouldUpdateAndReturnUpdatedOrderResponse() {
            UUID newProductId = UUID.randomUUID();
            Product newProduct = Product.builder()
                    .id(newProductId)
                    .ownerId(ownerId)
                    .name("Pizza")
                    .price(new BigDecimal("45.00"))
                    .status(ProductStatus.ACTIVE)
                    .build();

            OrderItemRequest newItemRequest = OrderItemRequest.builder()
                    .productId(newProductId)
                    .quantity(3)
                    .build();

            OrderRequest updateRequest = OrderRequest.builder()
                    .customerId(customerId)
                    .items(List.of(newItemRequest))
                    .build();

            OrderItem updatedItem = OrderItem.builder()
                    .id(UUID.randomUUID())
                    .product(newProduct)
                    .quantity(3)
                    .unitPrice(new BigDecimal("45.00"))
                    .unitCost(new BigDecimal("18.00"))
                    .build();

            Order updatedOrder = Order.builder()
                    .id(orderId)
                    .ownerId(ownerId)
                    .dateTime(order.getDateTime())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalValue(new BigDecimal("135.00"))
                    .estimatedProfit(new BigDecimal("81.00"))
                    .items(List.of(updatedItem))
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId)).willReturn(Optional.of(order));
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(newProductId, ownerId)).willReturn(Optional.of(newProduct));
            given(orderRepository.save(any(Order.class))).willReturn(updatedOrder);

            OrderResponse result = orderService.update(orderId, updateRequest);

            assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("135.00"));
            assertThat(result.getEstimatedProfit()).isEqualByComparingTo(new BigDecimal("81.00"));
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("deve recalcular totais ao atualizar pedido")
        void shouldRecalculateTotalsOnUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId)).willReturn(Optional.of(order));
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.update(orderId, orderRequest);

            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getTotalValue().compareTo(new BigDecimal("60.00")) == 0 &&
                    savedOrder.getEstimatedProfit().compareTo(new BigDecimal("36.00")) == 0 &&
                    savedOrder.getStatus() == OrderStatus.PAID
            ));
        }

        @Test
        @DisplayName("deve permitir alterar status do pedido quando status é informado")
        void shouldUpdateOrderStatusWhenProvided() {
            OrderRequest updateWithStatus = OrderRequest.builder()
                    .customerId(customerId)
                    .items(orderRequest.getItems())
                    .status(OrderStatus.CANCELLED)
                    .build();

            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId)).willReturn(Optional.of(order));
            given(customerRepository.findByIdAndOwnerId(customerId, ownerId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndOwnerId(productId, ownerId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.update(orderId, updateWithStatus);

            then(orderRepository).should().save(argThat(savedOrder -> savedOrder.getStatus() == OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException ao atualizar pedido inexistente")
        void shouldThrowWhenOrderNotFoundForUpdate() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.findByIdAndOwnerId(orderId, ownerId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.update(orderId, orderRequest))
                    .isInstanceOf(OrderNotFoundException.class);

            then(orderRepository).should(never()).save(any(Order.class));
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deve deletar pedido existente sem lançar exceção")
        void shouldDeleteExistingOrder() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.existsByIdAndOwnerId(orderId, ownerId)).willReturn(true);
            willDoNothing().given(orderRepository).deleteByIdAndOwnerId(orderId, ownerId);

            assertThatNoException().isThrownBy(() -> orderService.delete(orderId));

            then(orderRepository).should().deleteByIdAndOwnerId(orderId, ownerId);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException ao deletar pedido inexistente")
        void shouldThrowWhenOrderNotFoundForDelete() {
            given(userContext.getUserId()).willReturn(ownerId);
            given(orderRepository.existsByIdAndOwnerId(orderId, ownerId)).willReturn(false);

            assertThatThrownBy(() -> orderService.delete(orderId))
                    .isInstanceOf(OrderNotFoundException.class);

            then(orderRepository).should(never()).deleteByIdAndOwnerId(any(), any());
        }
    }
}

