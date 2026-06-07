package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.fee.Fee;
import com.MenuBank.MenuBank.fee.FeeRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import com.MenuBank.MenuBank.integration.IntegrationTestBase;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeKind;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderService — integração com Postgres")
class OrderServiceIntegrationTest extends IntegrationTestBase {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IncludeRepository includeRepository;
    @Autowired private FeeRepository feeRepository;

    private Merchant merchant;
    private Customer customer;
    private Product product;
    private Ingredient ingredient;

    @BeforeEach
    void setup() {
        merchant = createMerchantAndAuthenticate();
        Category category = categoryRepository.save(Category.builder()
                .merchant(merchant).name("Açaí").build());
        product = productRepository.save(Product.builder()
                .merchant(merchant).name("Açaí 500ml").price(new BigDecimal("20.00"))
                .status(ProductStatus.ACTIVE).category(category).build());
        customer = customerRepository.save(Customer.builder()
                .merchant(merchant).name("João").phone("11999990000").build());
        ingredient = ingredientRepository.save(Ingredient.builder()
                .merchant(merchant).name("leite ninho").canonicalName("leite ninho")
                .unit("g").costPerUnit(new BigDecimal("0.05"))
                .defaultQuantity(new BigDecimal("20"))
                .status(IngredientStatus.ACTIVE).build());
    }

    @Test
    @DisplayName("create deve persistir pedido com items, extras e calcular custo total")
    void create_shouldPersistOrderWithItemsAndExtras() {
        // PACKAGING "embalagem" 10un a 0.05 → base = 0.50 (sempre conta)
        includeRepository.save(Include.builder()
                .product(product).name("embalagem")
                .cost(new BigDecimal("0.05")).quantity(new BigDecimal("10"))
                .kind(IncludeKind.PACKAGING).build());
        // INGREDIENT na ficha técnica NÃO entra na base (só conta se for pedido)
        includeRepository.save(Include.builder()
                .product(product).name("ingrediente opcional caro")
                .cost(new BigDecimal("99.00")).quantity(new BigDecimal("10"))
                .kind(IncludeKind.INGREDIENT).build());

        OrderRequest request = OrderRequest.builder()
                .customerId(customer.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId())
                        .quantity(2)
                        .extraIngredients(List.of(OrderItemExtraIngredientRequest.builder()
                                .ingredientId(ingredient.getId())
                                .quantity(new BigDecimal("30")) // 30g de extra por unidade
                                .build()))
                        .build()))
                .build();

        OrderResponse response = orderService.create(merchant.getId(), request);

        // Assert response
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTotalValue()).isEqualByComparingTo("40.00"); // 20 × 2

        // Assert persistência
        Order persisted = orderRepository.findByIdAndMerchantId(response.getId(), merchant.getId()).orElseThrow();
        assertThat(persisted.getOrigin()).isEqualTo(OrderOrigin.MENUBANK);
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persisted.getItems()).hasSize(1);
        var item = persisted.getItems().get(0);
        assertThat(item.getExtraIngredients()).hasSize(1);
        assertThat(item.getExtraIngredients().get(0).getQuantity()).isEqualByComparingTo("30");
        // totalCost = (base PACKAGING 0.50 + extra 30×0.05=1.50) × 2 = 4.00
        // o include INGREDIENT (99×10) NÃO entra na base
        assertThat(persisted.getTotalCost()).isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("create deve aceitar pedido sem extras")
    void create_shouldAcceptOrderWithoutExtras() {
        OrderRequest request = OrderRequest.builder()
                .customerId(customer.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(1).build()))
                .build();

        OrderResponse response = orderService.create(merchant.getId(), request);

        Order persisted = orderRepository.findByIdAndMerchantId(response.getId(), merchant.getId()).orElseThrow();
        assertThat(persisted.getItems().get(0).getExtraIngredients()).isEmpty();
    }

    @Test
    @DisplayName("create deve falhar com cliente de outro merchant (isolamento)")
    void create_shouldRejectCustomerFromAnotherMerchant() {
        Merchant other = createMerchant("Outro");
        Customer fromOther = customerRepository.save(Customer.builder()
                .merchant(other).name("Outro Cliente").phone("999").build());

        OrderRequest request = OrderRequest.builder()
                .customerId(fromOther.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(1).build()))
                .build();

        assertThatThrownBy(() -> orderService.create(merchant.getId(), request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("findById deve retornar pedido do merchant autenticado")
    void findById_shouldReturnOrder() {
        OrderResponse created = orderService.create(merchant.getId(), simpleRequest());

        OrderResponse fetched = orderService.findById(merchant.getId(), created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getCustomerName()).isEqualTo("João");
    }

    @Test
    @DisplayName("findById deve falhar quando o pedido pertence a outro merchant")
    void findById_shouldNotLeakAcrossMerchants() {
        OrderResponse created = orderService.create(merchant.getId(), simpleRequest());

        // Autentica como outro merchant
        Merchant outro = createMerchant("Outro");

        assertThatThrownBy(() -> orderService.findById(outro.getId(), created.getId()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("update deve substituir items e recalcular custos")
    void update_shouldReplaceItemsAndRecalculateCost() {
        OrderResponse created = orderService.create(merchant.getId(), simpleRequest());

        OrderRequest updated = OrderRequest.builder()
                .customerId(customer.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(5).build()))
                .status(OrderStatus.PENDING)
                .build();

        OrderResponse response = orderService.update(merchant.getId(), created.getId(), updated);

        assertThat(response.getTotalValue()).isEqualByComparingTo("100.00"); // 20 × 5
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("delete deve remover pedido e cascatear items + extras")
    void delete_shouldRemoveOrderAndCascadeItems() {
        OrderResponse created = orderService.create(merchant.getId(), simpleRequest());
        UUID orderId = created.getId();

        orderService.delete(merchant.getId(), orderId);

        assertThat(orderRepository.findById(orderId)).isEmpty();
    }

    @Test
    @DisplayName("findAll deve paginar pedidos filtrando por nome do cliente")
    void findAll_shouldPaginateWithCustomerSearch() {
        Customer maria = customerRepository.save(Customer.builder()
                .merchant(merchant).name("Maria").phone("22222222222").build());

        orderService.create(merchant.getId(), simpleRequest());
        orderService.create(merchant.getId(), OrderRequest.builder()
                .customerId(maria.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(1).build()))
                .build());

        var page = orderService.findAll(merchant.getId(), "Maria", null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCustomerName()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("create com fee deve persistir referência à taxa")
    void create_shouldPersistFeeWhenProvided() {
        Fee fee = feeRepository.save(Fee.builder()
                .merchant(merchant).name("Pix").feeRate(new BigDecimal("0.0099")).build());

        OrderRequest request = OrderRequest.builder()
                .customerId(customer.getId())
                .feeId(fee.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(1).build()))
                .build();

        OrderResponse response = orderService.create(merchant.getId(), request);

        Order persisted = orderRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getFee().getId()).isEqualTo(fee.getId());
    }

    @Test
    @DisplayName("create deve aceitar taxa >= 10% (ex.: iFood 12%) e deduzi-la do lucro")
    void create_shouldAcceptFeeRateOfTenPercentOrMore() {
        // 12% não cabe em numeric(5,4); precisa de precisão maior para persistir.
        Fee fee = feeRepository.save(Fee.builder()
                .merchant(merchant).name("iFood").feeRate(new BigDecimal("12.00")).build());

        OrderRequest request = OrderRequest.builder()
                .customerId(customer.getId())
                .feeId(fee.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(1).build()))
                .build();

        OrderResponse response = orderService.create(merchant.getId(), request);

        Order persisted = orderRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getFee().getFeeRate()).isEqualByComparingTo("12.00");
        // totalValue 20.00, sem custo; taxa = 20 × 12% = 2.40 → lucro = 17.60
        assertThat(persisted.getTotalValue()).isEqualByComparingTo("20.00");
        assertThat(persisted.getEstimatedProfit()).isEqualByComparingTo("17.60");
    }

    private OrderRequest simpleRequest() {
        return OrderRequest.builder()
                .customerId(customer.getId())
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId()).quantity(1).build()))
                .build();
    }
}
