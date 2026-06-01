package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import com.MenuBank.MenuBank.fee.FeeRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeKind;
import com.MenuBank.MenuBank.product.IncludeRepository;
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
    private FeeRepository feeRepository;

    @Mock
    private IncludeRepository includeRepository;

    @Mock
    private com.MenuBank.MenuBank.product.OrderCostCalculatorService orderCostCalculatorService;

    @Mock
    private MerchantRepository merchantRepository;


    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private UUID merchantId;
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
        merchantId = UUID.randomUUID();
        lenient().when(merchantRepository.getReferenceById(any())).thenReturn(Merchant.builder().id(merchantId).build());
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();

        customer = Customer.builder()
                .id(customerId)
                .merchant(Merchant.builder().id(merchantId).build())
                .name("Cliente Teste")
                .phone("11999999999")
                .email("cliente@email.com")
                .build();

        product = Product.builder()
                .id(productId)
                .merchant(Merchant.builder().id(merchantId).build())
                .name("Hambúrguer")
                .price(new BigDecimal("30.00"))
                .status(ProductStatus.ACTIVE)
                .build();

        ingredient = Ingredient.builder()
                .id(ingredientId)
                .merchant(Merchant.builder().id(merchantId).build())
                .name("Bacon")
                .unit("g")
                .costPerUnit(new BigDecimal("0.10"))
                .status(IngredientStatus.ACTIVE)
                .build();

        // Mock receita do produto: 1 include (insumo/embalagem) × custo 12.00 = unitCost 12.00
        Include defaultRecipe = Include.builder()
                .product(product).name("CustoBase")
                .cost(new BigDecimal("12.00"))
                .quantity(BigDecimal.ONE)
                .kind(IncludeKind.PACKAGING).build();
        lenient().when(includeRepository.findByProductIdAndProductMerchantId(productId, merchantId))
                .thenReturn(List.of(defaultRecipe));

        // Cálculo padrão dos testes simples: 2 items × 12.00 base = 24.00.
        // Testes com extras sobrescrevem este mock.
        lenient().when(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                .thenReturn(new BigDecimal("24.00"));

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
                .merchant(Merchant.builder().id(merchantId).build())
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
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            OrderResponse result = orderService.create(merchantId, orderRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getCustomerId()).isEqualTo(customerId);
            assertThat(result.getCustomerName()).isEqualTo("Cliente Teste");
            then(orderRepository).should().save(any(Order.class));
        }

        @Test
        @DisplayName("deve definir status como PAID (concluído) por padrão")
        void shouldSetStatusToPaidByDefault() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(merchantId, orderRequest);

            then(orderRepository).should().save(argThat(savedOrder -> savedOrder.getStatus() == OrderStatus.PAID));
        }

        @Test
        @DisplayName("deve calcular totalValue a partir dos itens (quantidade × preço)")
        void shouldCalculateTotalValueFromItems() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(merchantId, orderRequest);

            // 2 × R$30.00 = R$60.00
            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getTotalValue().compareTo(new BigDecimal("60.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve calcular estimatedProfit (totalValue − custo total dos itens)")
        void shouldCalculateEstimatedProfitFromItems() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(merchantId, orderRequest);

            // totalValue(60.00) − totalCost(2 × 12.00 = 24.00) = 36.00
            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getEstimatedProfit().compareTo(new BigDecimal("36.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve definir unitPrice do item com o preço atual do produto")
        void shouldSetItemUnitPriceFromProductPrice() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.create(merchantId, orderRequest);

            then(orderRepository).should().save(argThat(savedOrder ->
                    savedOrder.getItems().get(0).getUnitPrice()
                            .compareTo(new BigDecimal("30.00")) == 0
            ));
        }

        @Test
        @DisplayName("deve definir dateTime no momento da criação")
        void shouldSetDateTimeOnCreation() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            LocalDateTime before = LocalDateTime.now();
            orderService.create(merchantId, orderRequest);
            LocalDateTime after = LocalDateTime.now();

            then(orderRepository).should().save(argThat(savedOrder ->
                    !savedOrder.getDateTime().isBefore(before) &&
                    !savedOrder.getDateTime().isAfter(after)
            ));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando cliente não encontrado")
        void shouldThrowWhenCustomerNotFound() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(merchantId, orderRequest))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Cliente");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando produto não encontrado")
        void shouldThrowWhenProductNotFound() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(merchantId, orderRequest))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Produto");

            then(orderRepository).should(never()).save(any(Order.class));
        }

        @Test
        @DisplayName("deve mapear corretamente os itens na resposta")
        void shouldMapItemsCorrectlyInResponse() {
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            OrderResponse result = orderService.create(merchantId, orderRequest);

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

            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndMerchantId(ingredientId, merchantId)).willReturn(Optional.of(ingredient));
            // Sobrescreve cálculo padrão com o valor esperado para este caso: base (24) + extras (10) = 34
            given(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                    .willReturn(new BigDecimal("34.00"));

            // return a saved copy so OrderResponse uses the calculated values
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(orderId);
                saved.getItems().forEach(i -> i.setId(UUID.randomUUID()));
                return saved;
            });

            OrderResponse response = orderService.create(merchantId, requestWithExtra);

            // totalValue = 2 * 30.00 = 60.00
            // totalCost (mockado pelo OrderCostCalculatorService) = 34.00
            // estimatedProfit = 60.00 - 34.00 = 26.00
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

            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndMerchantId(ingredientId, merchantId)).willReturn(Optional.of(ingredient));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(orderId);
                saved.getItems().forEach(i -> i.setId(UUID.randomUUID()));
                return saved;
            });
            // unitCost vem do calculator (mandatory base + opcionais escolhidos = 12 + 5 = 17)
            given(orderCostCalculatorService.computeItemUnitCost(any(OrderItem.class), eq(merchantId)))
                    .willReturn(new BigDecimal("17.00"));

            OrderResponse response = orderService.create(merchantId, requestWithExtra);

            // unitCost = base mandatória (12) + opcional escolhido (50 × 0.10 = 5) = 17
            // totalCost = unitCost × quantity (2) = 34
            OrderItemResponse itemResponse = response.getItems().get(0);
            assertThat(itemResponse.getUnitCost()).isEqualByComparingTo(new BigDecimal("17.00"));
            assertThat(itemResponse.getTotalCost()).isEqualByComparingTo(new BigDecimal("34.00"));
        }

        @Test
        @DisplayName("deve suportar costPerUnit com 4 casas decimais sem perder precisão")
        void shouldSupportFourDecimalCostPerUnit() {
            Ingredient fineIngredient = Ingredient.builder()
                    .id(ingredientId)
                    .merchant(Merchant.builder().id(merchantId).build())
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

            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndMerchantId(ingredientId, merchantId)).willReturn(Optional.of(fineIngredient));
            // totalCost esperado: base (12.00) + extra (100 * 0.0035 = 0.35) = 12.35
            given(orderCostCalculatorService.computeOrderTotalCost(any(Order.class)))
                    .willReturn(new BigDecimal("12.3500"));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(orderId);
                saved.getItems().forEach(i -> i.setId(UUID.randomUUID()));
                return saved;
            });

            OrderResponse response = orderService.create(merchantId, requestWithExtra);

            // totalValue = 30.00; totalCost = 12.35; estimatedProfit = 17.65
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

            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(ingredientRepository.findByIdAndMerchantId(ingredientId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(merchantId, requestWithExtra))
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
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(merchantId, orderId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(orderId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(result.getCustomerId()).isEqualTo(customerId);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException quando pedido não existe")
        void shouldThrowWhenOrderNotFound() {
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(merchantId, orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("deve popular insumos do item com os Includes da ficha técnica do produto")
        void shouldPopulateItemInsumosFromProductIncludes() {
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(merchantId, orderId);

            assertThat(result.getItems()).hasSize(1);
            OrderItemResponse item = result.getItems().get(0);
            assertThat(item.getInsumos())
                    .as("insumos devem refletir os Includes da ficha técnica do produto")
                    .hasSize(1);
            assertThat(item.getInsumos().get(0).getName()).isEqualTo("CustoBase");
            assertThat(item.getInsumos().get(0).getCost()).isEqualByComparingTo(new BigDecimal("12.00"));
            assertThat(item.getInsumos().get(0).getQuantity()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(item.getInsumos().get(0).getTotalCost()).isEqualByComparingTo(new BigDecimal("12.00"));
        }

        @Test
        @DisplayName("insumos NÃO devem incluir Includes do tipo INGREDIENT nem de kind nulo")
        void shouldExcludeIngredientKindIncludesFromInsumos() {
            Include packaging = Include.builder()
                    .product(product).name("Copo")
                    .cost(new BigDecimal("0.35")).quantity(BigDecimal.ONE)
                    .kind(IncludeKind.PACKAGING).build();
            Include specificIngredient = Include.builder()
                    .product(product).name("Creme de Ovomaltine")
                    .cost(new BigDecimal("0.80")).quantity(new BigDecimal("150"))
                    .kind(IncludeKind.INGREDIENT).build();
            Include legacyNullKind = Include.builder()
                    .product(product).name("Açaí Goat")
                    .cost(new BigDecimal("0.02")).quantity(new BigDecimal("150"))
                    .build();
            given(includeRepository.findByProductIdAndProductMerchantId(productId, merchantId))
                    .willReturn(List.of(packaging, specificIngredient, legacyNullKind));
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(merchantId, orderId);

            OrderItemResponse item = result.getItems().get(0);
            assertThat(item.getInsumos())
                    .as("apenas Includes PACKAGING entram em insumos")
                    .hasSize(1);
            assertThat(item.getInsumos().get(0).getName()).isEqualTo("Copo");
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
                    .merchant(Merchant.builder().id(merchantId).build())
                    .dateTime(LocalDateTime.now())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalValue(new BigDecimal("22.49"))
                    .estimatedProfit(new BigDecimal("-42.01"))
                    .items(new ArrayList<>(List.of(itemWithZeroCost)))
                    .build();

            given(orderRepository.findByIdAndMerchantId(orderId, merchantId))
                    .willReturn(Optional.of(orderWithStaleProfit));

            OrderResponse result = orderService.findById(merchantId, orderId);

            // 22.49 - (unitCost=0 × qty=1) - fee=0 = 22.49
            assertThat(result.getEstimatedProfit()).isEqualByComparingTo(new BigDecimal("22.49"));
        }

        @Test
        @DisplayName("deve retornar marginPct = estimatedProfit / totalValue * 100")
        void shouldReturnMarginPct() {
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));

            OrderResponse result = orderService.findById(merchantId, orderId);

            assertThat(result.getMarginPct()).isNotNull();
            // marginPct = estimatedProfit / totalValue * 100
            BigDecimal expected = result.getEstimatedProfit()
                    .divide(result.getTotalValue(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            assertThat(result.getMarginPct()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("deve retornar marginPct null quando totalValue é zero")
        void shouldReturnNullMarginWhenTotalValueIsZero() {
            Order zeroOrder = Order.builder()
                    .id(orderId)
                    .merchant(Merchant.builder().id(merchantId).build())
                    .dateTime(LocalDateTime.now())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalValue(BigDecimal.ZERO)
                    .estimatedProfit(BigDecimal.ZERO)
                    .items(new ArrayList<>())
                    .build();

            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(zeroOrder));

            OrderResponse result = orderService.findById(merchantId, orderId);

            assertThat(result.getMarginPct()).isNull();
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
            given(orderRepository.findPageByMerchantIdAndCustomerNameContaining(merchantId, "client", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(order), pageable, 1));

            org.springframework.data.domain.Page<OrderResponse> result =
                    orderService.findAll(merchantId, "client", null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(orderId);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve tratar search nulo como string vazia")
        void shouldTreatNullSearchAsEmpty() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(orderRepository.findPageByMerchantIdAndCustomerNameContaining(merchantId, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));

            org.springframework.data.domain.Page<OrderResponse> result =
                    orderService.findAll(merchantId, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("deve filtrar por status quando informado")
        void shouldFilterByStatusWhenProvided() {
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(0, 20);
            given(orderRepository.findPageByMerchantIdAndStatusAndCustomerNameContaining(
                    merchantId, OrderStatus.READY, "", pageable))
                    .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(order), pageable, 1));

            org.springframework.data.domain.Page<OrderResponse> result =
                    orderService.findAll(merchantId, null, OrderStatus.READY, pageable);

            assertThat(result.getContent()).hasSize(1);
            then(orderRepository).should(never())
                    .findPageByMerchantIdAndCustomerNameContaining(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("statusCounts(start, end, search)")
    class StatusCounts {

        @Test
        @DisplayName("deve retornar contagens por status, preenchendo zeros para valores ausentes")
        void shouldReturnCountsByStatusWithZeroFill() {
            given(orderRepository.countByStatusForMerchant(eq(merchantId), any(), any(), eq("")))
                    .willReturn(List.of(
                            new Object[]{OrderStatus.PENDING, 5L},
                            new Object[]{OrderStatus.READY, 2L}
                    ));

            java.util.Map<OrderStatus, Long> result = orderService.statusCounts(merchantId, null, null, null);

            assertThat(result).containsEntry(OrderStatus.PENDING, 5L);
            assertThat(result).containsEntry(OrderStatus.READY, 2L);
            assertThat(result).containsEntry(OrderStatus.DELIVERED, 0L);
            assertThat(result).containsEntry(OrderStatus.PAID, 0L);
            assertThat(result).containsEntry(OrderStatus.CANCELLED, 0L);
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
                    .merchant(Merchant.builder().id(merchantId).build())
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
                    .merchant(Merchant.builder().id(merchantId).build())
                    .dateTime(order.getDateTime())
                    .customer(customer)
                    .status(OrderStatus.PENDING)
                    .totalValue(new BigDecimal("135.00"))
                    .estimatedProfit(new BigDecimal("81.00"))
                    .items(List.of(updatedItem))
                    .build();

            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(newProductId, merchantId)).willReturn(Optional.of(newProduct));
            given(orderRepository.save(any(Order.class))).willReturn(updatedOrder);

            OrderResponse result = orderService.update(merchantId, orderId, updateRequest);

            assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("135.00"));
            assertThat(result.getEstimatedProfit()).isEqualByComparingTo(new BigDecimal("81.00"));
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("deve recalcular totais ao atualizar pedido")
        void shouldRecalculateTotalsOnUpdate() {
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.update(merchantId, orderId, orderRequest);

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

            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.of(order));
            given(customerRepository.findByIdAndMerchantId(customerId, merchantId)).willReturn(Optional.of(customer));
            given(productRepository.findByIdAndMerchantId(productId, merchantId)).willReturn(Optional.of(product));
            given(orderRepository.save(any(Order.class))).willReturn(order);

            orderService.update(merchantId, orderId, updateWithStatus);

            then(orderRepository).should().save(argThat(savedOrder -> savedOrder.getStatus() == OrderStatus.CANCELLED));
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException ao atualizar pedido inexistente")
        void shouldThrowWhenOrderNotFoundForUpdate() {
            given(orderRepository.findByIdAndMerchantId(orderId, merchantId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.update(merchantId, orderId, orderRequest))
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
            given(orderRepository.existsByIdAndMerchantId(orderId, merchantId)).willReturn(true);
            willDoNothing().given(orderRepository).deleteByIdAndMerchantId(orderId, merchantId);

            assertThatNoException().isThrownBy(() -> orderService.delete(merchantId, orderId));

            then(orderRepository).should().deleteByIdAndMerchantId(orderId, merchantId);
        }

        @Test
        @DisplayName("deve lançar OrderNotFoundException ao deletar pedido inexistente")
        void shouldThrowWhenOrderNotFoundForDelete() {
            given(orderRepository.existsByIdAndMerchantId(orderId, merchantId)).willReturn(false);

            assertThatThrownBy(() -> orderService.delete(merchantId, orderId))
                    .isInstanceOf(OrderNotFoundException.class);

            then(orderRepository).should(never()).deleteByIdAndMerchantId(any(), any());
        }
    }
}

