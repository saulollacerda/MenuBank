package com.MenuBank.MenuBank.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * Lucro estimado do pedido = (totalValue − deliveryFee) − totalCost − taxa.
     * <p>
     * Todos os campos podem ser {@code null}; tratados como zero. A taxa de entrega
     * é deduzida porque é repassada ao entregador/plataforma e não fica com o
     * restaurante. A taxa de meio de pagamento ({@code fee.feeRate}, em %) incide
     * sobre o subtotal dos produtos ({@code totalValue − deliveryFee}) e também é
     * deduzida, pois é cobrada pela plataforma/adquirente.
     */
    public static BigDecimal calculateEstimatedProfit(Order order) {
        if (order == null) return BigDecimal.ZERO;
        BigDecimal feeRate = order.getFee() != null ? order.getFee().getFeeRate() : null;
        return calculateEstimatedProfit(order.getTotalValue(), order.getDeliveryFee(),
                order.getTotalCost(), feeRate);
    }

    /**
     * @deprecated use {@link #calculateEstimatedProfit(BigDecimal, BigDecimal, BigDecimal, BigDecimal)}
     * para incluir a taxa de meio de pagamento. Mantido para compatibilidade.
     */
    @Deprecated
    public static BigDecimal calculateEstimatedProfit(BigDecimal totalValue,
                                                       BigDecimal deliveryFee,
                                                       BigDecimal totalCost) {
        return calculateEstimatedProfit(totalValue, deliveryFee, totalCost, null);
    }

    /**
     * Overload usado em {@code toResponse}, quando {@code totalCost} já foi resolvido
     * a partir do snapshot ou recalculado a partir dos items (fallback legado).
     *
     * @param feeRate percentual da taxa de meio de pagamento (ex.: {@code 2.5} = 2,5%);
     *                {@code null}/zero = sem taxa.
     */
    public static BigDecimal calculateEstimatedProfit(BigDecimal totalValue,
                                                       BigDecimal deliveryFee,
                                                       BigDecimal totalCost,
                                                       BigDecimal feeRate) {
        BigDecimal subtotal = nz(totalValue).subtract(nz(deliveryFee));
        BigDecimal feeAmount = subtotal
                .multiply(nz(feeRate))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return subtotal.subtract(nz(totalCost)).subtract(feeAmount);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
