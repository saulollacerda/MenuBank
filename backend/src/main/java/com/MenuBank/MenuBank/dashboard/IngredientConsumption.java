package com.MenuBank.MenuBank.dashboard;

import lombok.*;

import java.math.BigDecimal;

/**
 * Aggregated consumption of a single ingredient over a period: how much of it was
 * consumed (total quantity, in the ingredient's own unit) and what it cost the merchant.
 *
 * <p>Combines both per-order consumption sources: the order ficha snapshot
 * ({@link com.MenuBank.MenuBank.order.OrderFichaIngredient}, once per order) and the
 * item extras ({@link com.MenuBank.MenuBank.order.OrderItemExtraIngredient}, multiplied
 * by the item quantity).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientConsumption {

    private String ingredientName;
    private String unit;
    private BigDecimal totalQuantity;
    private BigDecimal totalCost;
}
