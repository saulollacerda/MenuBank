package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.payment.PaymentMethod;

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

    public static BigDecimal calculateEstimatedProfit(BigDecimal totalValue,
                                                      BigDecimal totalCost,
                                                      PaymentMethod paymentMethod) {
        return calculateEstimatedProfit(totalValue, totalCost, paymentMethod, BigDecimal.ZERO);
    }

    /**
     * Calcula o lucro estimado descontando a taxa de entrega da receita efetiva.
     * O {@code deliveryFee} é repassado ao entregador/plataforma e não compõe receita do restaurante.
     * A taxa do meio de pagamento incide sobre o {@code totalValue} (o valor que efetivamente passa pela maquininha/PSP).
     */
    public static BigDecimal calculateEstimatedProfit(BigDecimal totalValue,
                                                      BigDecimal totalCost,
                                                      PaymentMethod paymentMethod,
                                                      BigDecimal deliveryFee) {
        BigDecimal feeAmount = BigDecimal.ZERO;
        if (paymentMethod != null && paymentMethod.getFeeRate().compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = totalValue.multiply(paymentMethod.getFeeRate())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        BigDecimal delivery = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        BigDecimal effectiveRevenue = totalValue.subtract(delivery);
        return effectiveRevenue.subtract(totalCost).subtract(feeAmount);
    }
}
