package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderCostCalculatorService — modelo aditivo")
class OrderCostCalculatorServiceTest {

    private OrderCostCalculatorService calc;
    private UUID ownerId;
    private Product product;

    @BeforeEach
    void setUp() {
        calc = new OrderCostCalculatorService();
        ownerId = UUID.randomUUID();
        product = Product.builder().id(UUID.randomUUID()).ownerId(ownerId).name("Açaí").build();
    }

    private OrderItem item(int quantity, BigDecimal unitCost, List<OrderItemExtraIngredient> extras) {
        return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(new BigDecimal("20.00"))
                .unitCost(unitCost)
                .extraIngredients(extras)
                .build();
    }

    private OrderItemExtraIngredient extra(BigDecimal qty, BigDecimal costPerUnit) {
        return OrderItemExtraIngredient.builder()
                .quantity(qty).costPerUnit(costPerUnit)
                .ingredientName("x").ingredientUnit("g")
                .build();
    }

    @Test
    @DisplayName("computeItemUnitCost = item.unitCost + sum(extras.quantity × costPerUnit)")
    void itemUnitCostIsAdditive() {
        OrderItem i = item(1, new BigDecimal("5.00"), List.of(
                extra(new BigDecimal("20"), new BigDecimal("0.10")),
                extra(new BigDecimal("40"), new BigDecimal("0.0533"))
        ));

        BigDecimal unit = calc.computeItemUnitCost(i, ownerId);

        // 5 + (20*0.10) + (40*0.0533) = 5 + 2 + 2.132 = 9.132
        assertThat(unit).isEqualByComparingTo("9.132");
    }

    @Test
    @DisplayName("computeItemUnitCost trata item.unitCost nulo como zero")
    void itemUnitCostHandlesNullUnitCost() {
        OrderItem i = item(1, null, List.of(extra(new BigDecimal("10"), new BigDecimal("0.50"))));
        assertThat(calc.computeItemUnitCost(i, ownerId)).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("computeItemUnitCost sem extras = item.unitCost")
    void itemUnitCostWithoutExtras() {
        OrderItem i = item(1, new BigDecimal("7.50"), List.of());
        assertThat(calc.computeItemUnitCost(i, ownerId)).isEqualByComparingTo("7.50");
    }

    @Test
    @DisplayName("computeOrderTotalCost = sum(computeItemUnitCost × quantity)")
    void orderTotalCostMultipliesByQuantity() {
        OrderItem i = item(2, new BigDecimal("3.00"), List.of(
                extra(new BigDecimal("10"), new BigDecimal("0.20"))
        ));
        Order order = Order.builder().ownerId(ownerId).items(List.of(i)).build();

        // unitCost = 3 + 10*0.20 = 5.00; total = 5.00 × 2 = 10.00
        assertThat(calc.computeOrderTotalCost(order)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("computeOrderTotalCost soma vários itens do pedido")
    void orderTotalCostSumsMultipleItems() {
        OrderItem a = item(1, new BigDecimal("5.00"), List.of());
        OrderItem b = item(3, new BigDecimal("2.00"), List.of(extra(BigDecimal.ONE, BigDecimal.ONE)));
        Order order = Order.builder().ownerId(ownerId).items(List.of(a, b)).build();

        // a: 5.00; b: (2 + 1) × 3 = 9.00; total = 14.00
        assertThat(calc.computeOrderTotalCost(order)).isEqualByComparingTo("14.00");
    }

    @Test
    @DisplayName("computeOrderTotalCost retorna zero para pedido sem itens ou null")
    void orderTotalCostReturnsZeroForEmptyOrNull() {
        assertThat(calc.computeOrderTotalCost(null)).isEqualByComparingTo("0");
        assertThat(calc.computeOrderTotalCost(Order.builder().ownerId(ownerId).items(List.of()).build()))
                .isEqualByComparingTo("0");
    }
}
