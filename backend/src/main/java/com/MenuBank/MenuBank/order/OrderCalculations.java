package com.MenuBank.MenuBank.order;

import java.math.BigDecimal;
import java.util.List;

public final class OrderCalculations {

    private OrderCalculations() {
    }

    public static BigDecimal calculateTotalCost(List<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal cost = item.getUnitCost();
                    if (cost == null) {
                        cost = BigDecimal.ZERO;
                    }
                    BigDecimal baseCost = cost.multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal extraCost = calculateExtraIngredientsCost(item);
                    return baseCost.add(extraCost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal calculateExtraIngredientsCost(OrderItem item) {
        if (item.getExtraIngredients() == null || item.getExtraIngredients().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal perUnitExtraCost = item.getExtraIngredients().stream()
                .map(extra -> extra.getQuantity().multiply(extra.getCostPerUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return perUnitExtraCost.multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    /**
     * Lucro estimado do pedido = totalValue − deliveryFee − totalCost.
     * <p>
     * Todos os campos podem ser {@code null}; tratados como zero. A taxa de entrega
     * é deduzida porque é repassada ao entregador/plataforma e não fica com o
     * restaurante. Taxas de meio de pagamento ainda NÃO entram nesta fórmula.
     */
    public static BigDecimal calculateEstimatedProfit(Order order) {
        if (order == null) return BigDecimal.ZERO;
        return calculateEstimatedProfit(order.getTotalValue(), order.getDeliveryFee(), order.getTotalCost());
    }

    /**
     * Overload usado em {@code toResponse}, quando {@code totalCost} já foi resolvido
     * a partir do snapshot ou recalculado a partir dos items (fallback legado).
     */
    public static BigDecimal calculateEstimatedProfit(BigDecimal totalValue,
                                                       BigDecimal deliveryFee,
                                                       BigDecimal totalCost) {
        return nz(totalValue).subtract(nz(deliveryFee)).subtract(nz(totalCost));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
