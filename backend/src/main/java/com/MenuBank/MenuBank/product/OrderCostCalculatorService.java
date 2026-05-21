package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calcula o custo total estimado de um {@link Order} com base nos {@link ProductIngredient}
 * configurados para cada {@link Product}.
 *
 * <p>Para cada {@link OrderItem}:</p>
 * <ol>
 *   <li>Soma {@code grammage × costPerUnit} de todos os ingredientes obrigatórios
 *       ({@code isOptional=false}) — a base do produto sempre entra no custo.</li>
 *   <li>Para cada {@link OrderItemExtraIngredient} do pedido (representa o {@code subItem}
 *       que o cliente escolheu), soma o custo se aquele ingrediente também estiver listado
 *       como opcional ({@code isOptional=true}) na receita do produto.</li>
 *   <li>Multiplica o custo unitário pela {@code quantity} do item.</li>
 * </ol>
 *
 * <p>Bordas tratadas:</p>
 * <ul>
 *   <li>Ingrediente sem {@code costPerUnit} (null) → trata como zero (não bloqueia)</li>
 *   <li>Extra com {@code ingredient=null} (subItem sem mapeamento, ex: iFood) → ignorado</li>
 *   <li>Produto sem {@code ProductIngredient} → custo do item = 0</li>
 * </ul>
 */
@Service
public class OrderCostCalculatorService {

    private final ProductIngredientRepository productIngredientRepository;

    public OrderCostCalculatorService(ProductIngredientRepository productIngredientRepository) {
        this.productIngredientRepository = productIngredientRepository;
    }

    public BigDecimal computeOrderTotalCost(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return order.getItems().stream()
                .map(item -> computeItemCost(item, order.getOwnerId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeItemCost(OrderItem item, UUID ownerId) {
        List<ProductIngredient> productIngredients = productIngredientRepository
                .findByProductIdAndProductOwnerId(item.getProduct().getId(), ownerId);

        if (productIngredients.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Index por ingredientId para lookup rápido dos opcionais escolhidos via subItems
        Map<UUID, ProductIngredient> byIngredientId = new HashMap<>();
        BigDecimal mandatoryCost = BigDecimal.ZERO;

        for (ProductIngredient pi : productIngredients) {
            byIngredientId.put(pi.getIngredient().getId(), pi);
            if (!pi.isOptional()) {
                mandatoryCost = mandatoryCost.add(costOf(pi));
            }
        }

        // Opcionais: somar apenas os que aparecem nos subItems (OrderItemExtraIngredient)
        BigDecimal optionalCost = BigDecimal.ZERO;
        if (item.getExtraIngredients() != null) {
            for (OrderItemExtraIngredient extra : item.getExtraIngredients()) {
                Ingredient ing = extra.getIngredient();
                if (ing == null) continue;
                ProductIngredient match = byIngredientId.get(ing.getId());
                if (match != null && match.isOptional()) {
                    optionalCost = optionalCost.add(costOf(match));
                }
            }
        }

        BigDecimal unitCost = mandatoryCost.add(optionalCost);
        return unitCost.multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private BigDecimal costOf(ProductIngredient pi) {
        BigDecimal costPerUnit = pi.getIngredient().getCostPerUnit();
        if (costPerUnit == null) costPerUnit = BigDecimal.ZERO;
        return pi.getGrammage().multiply(costPerUnit);
    }
}
