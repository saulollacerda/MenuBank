package com.MenuBank.MenuBank.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utilitário para calcular o custo unitário estimado de um produto
 * a partir da soma do custo dos seus ingredientes (RecipeItems).
 *
 * Usado como snapshot no momento da criação de OrderItems — assim o custo
 * histórico de cada pedido permanece consistente mesmo se a receita do
 * produto mudar depois.
 */
public final class ProductCostCalculator {

    private ProductCostCalculator() {
    }

    /**
     * Soma (quantidade × custo por unidade) de cada ingrediente da receita.
     * Retorna {@link BigDecimal#ZERO} se a lista estiver vazia ou nula.
     */
    public static BigDecimal computeUnitCost(List<RecipeItem> recipeItems) {
        if (recipeItems == null || recipeItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return recipeItems.stream()
                .map(i -> i.getQuantity().multiply(i.getIngredient().getCostPerUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
