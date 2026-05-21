package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCostCalculatorService")
class OrderCostCalculatorServiceTest {

    @Mock private ProductIngredientRepository productIngredientRepository;
    @Mock private IngredientRepository ingredientRepository;

    @InjectMocks
    private OrderCostCalculatorService orderCostCalculator;

    private UUID ownerId;
    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = Product.builder()
                .id(productId).ownerId(ownerId).name("Açaí 500ml")
                .status(ProductStatus.ACTIVE).build();
    }

    private Ingredient ingredient(String name, BigDecimal costPerUnit, String externalId) {
        return Ingredient.builder()
                .id(UUID.randomUUID()).ownerId(ownerId).name(name)
                .unit("g").costPerUnit(costPerUnit).status(IngredientStatus.ACTIVE)
                .externalId(externalId)
                .build();
    }

    private ProductIngredient pi(Ingredient ing, BigDecimal grammage, boolean isOptional) {
        return ProductIngredient.builder()
                .id(UUID.randomUUID())
                .product(product).ingredient(ing).grammage(grammage).isOptional(isOptional)
                .build();
    }

    private OrderItem orderItem(int quantity, List<OrderItemExtraIngredient> extras) {
        OrderItem item = OrderItem.builder()
                .product(product).quantity(quantity)
                .unitPrice(new BigDecimal("21.99")).unitCost(BigDecimal.ZERO)
                .build();
        if (extras != null) {
            extras.forEach(e -> e.setOrderItem(item));
            item.setExtraIngredients(extras);
        }
        return item;
    }

    private OrderItemExtraIngredient extra(Ingredient ing) {
        return OrderItemExtraIngredient.builder()
                .ingredient(ing).ingredientName(ing.getName()).ingredientUnit(ing.getUnit())
                .quantity(BigDecimal.ONE).costPerUnit(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("deve somar custo de todos os ingredientes obrigatórios (isOptional=false)")
    void computeOrderTotalCost_shouldSumCostOfMandatoryIngredients() {
        Ingredient acaiBase = ingredient("Açaí Base", new BigDecimal("0.05"), "ing-acai");
        Ingredient copo = ingredient("Copo", new BigDecimal("0.30"), "ing-copo");

        OrderItem item = orderItem(1, List.of());
        Order order = Order.builder().ownerId(ownerId).items(List.of(item)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of(
                        pi(acaiBase, new BigDecimal("400"), false),
                        pi(copo, BigDecimal.ONE, false)
                ));

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        // (400 * 0.05) + (1 * 0.30) = 20.00 + 0.30 = 20.30
        assertThat(cost).isEqualByComparingTo("20.30");
    }

    @Test
    @DisplayName("deve adicionar custo de ingredientes opcionais apenas quando aparecem nos subItems do pedido")
    void computeOrderTotalCost_shouldAddCostOfOptionalIngredientsPresentInSubItems() {
        Ingredient acaiBase = ingredient("Açaí Base", new BigDecimal("0.05"), "ing-acai");
        Ingredient granola = ingredient("Granola", new BigDecimal("0.10"), "ing-granola");
        Ingredient leiteNinho = ingredient("Leite Ninho", new BigDecimal("0.20"), "ing-ninho");

        OrderItem item = orderItem(1, List.of(extra(granola)));  // só granola foi escolhida
        Order order = Order.builder().ownerId(ownerId).items(List.of(item)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of(
                        pi(acaiBase, new BigDecimal("400"), false),
                        pi(granola, new BigDecimal("30"), true),
                        pi(leiteNinho, new BigDecimal("20"), true)   // opcional mas NÃO está nos extras
                ));

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        // obrigatório: 400 * 0.05 = 20.00
        // opcional presente: 30 * 0.10 = 3.00
        // opcional ausente (Leite Ninho): ignorado
        // total: 23.00
        assertThat(cost).isEqualByComparingTo("23.00");
    }

    @Test
    @DisplayName("deve ignorar extras com ingrediente nulo (subItem sem internalId, ex: iFood)")
    void computeOrderTotalCost_shouldIgnoreExtrasWithNullIngredient() {
        Ingredient acaiBase = ingredient("Açaí Base", new BigDecimal("0.05"), "ing-acai");

        OrderItemExtraIngredient nullExtra = OrderItemExtraIngredient.builder()
                .ingredient(null).ingredientName("Não mapeado")
                .quantity(BigDecimal.ONE).costPerUnit(BigDecimal.ZERO).build();

        OrderItem item = orderItem(1, List.of(nullExtra));
        Order order = Order.builder().ownerId(ownerId).items(List.of(item)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of(pi(acaiBase, new BigDecimal("400"), false)));

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        // só a base obrigatória: 20.00
        assertThat(cost).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("deve tratar ingrediente sem costPerUnit como custo zero (não lança)")
    void computeOrderTotalCost_shouldTreatNullCostPerUnitAsZero() {
        Ingredient acaiBase = ingredient("Açaí Base", null, "ing-acai");  // sem custo

        OrderItem item = orderItem(1, List.of());
        Order order = Order.builder().ownerId(ownerId).items(List.of(item)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of(pi(acaiBase, new BigDecimal("400"), false)));

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        assertThat(cost).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("deve multiplicar o custo do item pela quantity (2 açaís = 2× o custo)")
    void computeOrderTotalCost_shouldMultiplyByItemQuantity() {
        Ingredient acaiBase = ingredient("Açaí Base", new BigDecimal("0.05"), "ing-acai");

        OrderItem item = orderItem(2, List.of());  // 2× o mesmo produto
        Order order = Order.builder().ownerId(ownerId).items(List.of(item)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of(pi(acaiBase, new BigDecimal("400"), false)));

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        // 2 * (400 * 0.05) = 2 * 20.00 = 40.00
        assertThat(cost).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("deve somar custos de múltiplos OrderItems (pedido com vários produtos)")
    void computeOrderTotalCost_shouldSumAllItems() {
        Ingredient acai = ingredient("Açaí Base", new BigDecimal("0.05"), "ing-acai");
        UUID product2Id = UUID.randomUUID();
        Product product2 = Product.builder().id(product2Id).ownerId(ownerId).name("Suco").build();
        Ingredient suco = ingredient("Suco Concentrado", new BigDecimal("0.10"), "ing-suco");

        OrderItem item1 = orderItem(1, List.of());
        OrderItem item2 = OrderItem.builder()
                .product(product2).quantity(1).unitPrice(BigDecimal.TEN).unitCost(BigDecimal.ZERO).build();

        Order order = Order.builder().ownerId(ownerId).items(List.of(item1, item2)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of(pi(acai, new BigDecimal("400"), false)));
        given(productIngredientRepository.findByProductIdAndProductOwnerId(product2Id, ownerId))
                .willReturn(List.of(
                        ProductIngredient.builder().id(UUID.randomUUID()).product(product2)
                                .ingredient(suco).grammage(new BigDecimal("50")).isOptional(false).build()
                ));

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        // item1: 400 * 0.05 = 20.00
        // item2: 50 * 0.10 = 5.00
        // total: 25.00
        assertThat(cost).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("deve retornar zero quando produto não tem ProductIngredient associado")
    void computeOrderTotalCost_shouldReturnZeroForProductWithoutIngredients() {
        OrderItem item = orderItem(1, List.of());
        Order order = Order.builder().ownerId(ownerId).items(List.of(item)).build();

        given(productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId))
                .willReturn(List.of());

        BigDecimal cost = orderCostCalculator.computeOrderTotalCost(order);

        assertThat(cost).isEqualByComparingTo("0");
    }
}
