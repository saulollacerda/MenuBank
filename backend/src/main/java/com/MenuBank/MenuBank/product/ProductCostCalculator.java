package com.MenuBank.MenuBank.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utilitário legado para calcular o custo unitário base de um produto a partir dos
 * ingredientes obrigatórios (isOptional=false) da sua ficha técnica.
 *
 * <p><b>Deprecated em favor de {@code OrderCostCalculatorService}</b> — este utilitário
 * só considera a base obrigatória e é mantido como snapshot do {@code unitCost} no
 * {@code OrderItem}. Para o cálculo completo do custo do pedido (base + opcionais
 * presentes nos subItems), use o service.</p>
 */
public final class ProductCostCalculator {

    private ProductCostCalculator() {
    }

    /**
     * Soma {@code grammage × costPerUnit} dos ingredientes obrigatórios (isOptional=false).
     * Ingredientes opcionais são ignorados aqui — entram no cálculo só quando aparecem
     * nos subItems do pedido (responsabilidade do {@code OrderCostCalculatorService}).
     *
     * Retorna {@link BigDecimal#ZERO} se a lista estiver vazia/nula ou se o ingrediente
     * tiver {@code costPerUnit} nulo (tratado como zero).
     */
    public static BigDecimal computeUnitCost(List<ProductIngredient> productIngredients) {
        if (productIngredients == null || productIngredients.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return productIngredients.stream()
                .filter(pi -> !pi.isOptional())
                .map(pi -> {
                    BigDecimal cost = pi.getIngredient().getCostPerUnit();
                    if (cost == null) cost = BigDecimal.ZERO;
                    return pi.getGrammage().multiply(cost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
