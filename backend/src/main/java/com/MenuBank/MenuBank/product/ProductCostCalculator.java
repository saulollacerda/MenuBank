package com.MenuBank.MenuBank.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utilitario para calcular custos a partir dos {@link Include}s (componentes da ficha
 * tecnica) de um produto.
 *
 * <ul>
 *   <li>{@link #computeUnitCost(List)} — custo teorico da receita completa (todos os
 *       includes). Usado no catalogo de produtos (custo/margem do produto).</li>
 *   <li>{@link #computeOrderBaseCost(List)} — custo base que entra em todo pedido:
 *       apenas {@link IncludeKind#PACKAGING} (copo, colher, embalagem...). Ingredientes
 *       sao opcoes de personalizacao e so geram custo quando pedidos (via
 *       {@code OrderItemExtraIngredient}).</li>
 * </ul>
 */
public final class ProductCostCalculator {

    private ProductCostCalculator() {
    }

    /**
     * Soma {@code cost x quantity} de TODOS os {@link Include}s do produto (receita completa).
     * Retorna {@link BigDecimal#ZERO} se a lista estiver vazia/nula.
     */
    public static BigDecimal computeUnitCost(List<Include> includes) {
        return sum(includes, inc -> true);
    }

    /**
     * Soma {@code cost x quantity} apenas dos {@link Include}s {@link IncludeKind#PACKAGING}.
     * Esse e o custo base de um item de pedido: o que esta sempre presente independentemente
     * do que o cliente escolheu. Includes {@code INGREDIENT} (ou legados sem kind) ficam de
     * fora — so contam quando o cliente os pede.
     * Retorna {@link BigDecimal#ZERO} se a lista estiver vazia/nula ou sem PACKAGING.
     */
    public static BigDecimal computeOrderBaseCost(List<Include> includes) {
        return sum(includes, inc -> inc.getKind() == IncludeKind.PACKAGING);
    }

    private static BigDecimal sum(List<Include> includes, java.util.function.Predicate<Include> filter) {
        if (includes == null || includes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return includes.stream()
                .filter(filter)
                .map(inc -> {
                    BigDecimal cost = inc.getCost() != null ? inc.getCost() : BigDecimal.ZERO;
                    BigDecimal qty = inc.getQuantity() != null ? inc.getQuantity() : BigDecimal.ONE;
                    return cost.multiply(qty);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
