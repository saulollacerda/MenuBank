package com.MenuBank.MenuBank.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utilitario para calcular o custo unitario base de um produto a partir dos seus
 * {@link Include}s (componentes da ficha tecnica).
 *
 * <p>No modelo atual, todos os includes sempre contam para o custo do produto.
 * O custo total e: {@code sum(include.cost x include.quantity)}.</p>
 */
public final class ProductCostCalculator {

    private ProductCostCalculator() {
    }

    /**
     * Soma {@code cost x quantity} de todos os {@link Include}s do produto.
     * Retorna {@link BigDecimal#ZERO} se a lista estiver vazia/nula.
     */
    public static BigDecimal computeUnitCost(List<Include> includes) {
        if (includes == null || includes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return includes.stream()
                .map(inc -> {
                    BigDecimal cost = inc.getCost() != null ? inc.getCost() : BigDecimal.ZERO;
                    BigDecimal qty = inc.getQuantity() != null ? inc.getQuantity() : BigDecimal.ONE;
                    return cost.multiply(qty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
