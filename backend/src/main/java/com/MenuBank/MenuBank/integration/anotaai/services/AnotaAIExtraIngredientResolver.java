package com.MenuBank.MenuBank.integration.anotaai.services;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNameNormalizer;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.integration.anotaai.AnotaAIOrderDetailResponse;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import com.MenuBank.MenuBank.product.Include;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolve os subItems de um pedido em {@link OrderItemExtraIngredient}s.
 *
 * <p><b>Deduplicação:</b> a Anota.AI pode enviar o mesmo ingrediente em grupos
 * distintos (adicionais básicos + adicionais extras). Antes de resolver, agrega
 * todos os subItems pelo nome canônico, somando as quantities.
 *
 * <p><b>Include é autoritativo:</b> se o subItem casa com um {@link Include} da
 * ficha técnica do produto (mesmo nome normalizado), ele é considerado parte da
 * receita base — <b>nenhum {@code OrderItemExtraIngredient} é criado</b>. Evita
 * duplicação no detalhe do pedido (mesmo ingrediente em Insumos + Extras) e
 * double-counting no cálculo de custo.
 *
 * <p>Quando não há Include correspondente, o subItem vira um extra usando
 * {@code Ingredient.defaultQuantity} (qty global) × {@code subItem.quantity} e o
 * {@code Ingredient.costPerUnit} global.
 */
public class AnotaAIExtraIngredientResolver {

    private final IngredientRepository ingredientRepository;
    private final NotificationService notificationService;

    public AnotaAIExtraIngredientResolver(IngredientRepository ingredientRepository,
                                           NotificationService notificationService) {
        this.ingredientRepository = ingredientRepository;
        this.notificationService = notificationService;
    }

    public List<OrderItemExtraIngredient> resolve(
            List<AnotaAIOrderDetailResponse.AnotaAISubItem> subItems,
            List<Include> productIncludes,
            UUID merchantId,
            Set<String> missingIngredientNames) {
        if (subItems == null || subItems.isEmpty()) return new ArrayList<>();

        Map<String, AnotaAIOrderDetailResponse.AnotaAISubItem> merged = new LinkedHashMap<>();
        for (AnotaAIOrderDetailResponse.AnotaAISubItem subItem : subItems) {
            String rawName = subItem.getName();
            if (rawName == null || rawName.isBlank()) continue;
            String canonical = IngredientNameNormalizer.normalize(rawName);
            merged.merge(canonical, subItem, (existing, incoming) -> {
                existing.setQuantity(existing.getQuantity() + incoming.getQuantity());
                return existing;
            });
        }

        List<OrderItemExtraIngredient> extras = new ArrayList<>();
        for (Map.Entry<String, AnotaAIOrderDetailResponse.AnotaAISubItem> entry : merged.entrySet()) {
            String canonical = entry.getKey();
            AnotaAIOrderDetailResponse.AnotaAISubItem subItem = entry.getValue();

            // Include autoritativo: se o subItem casa com um item da ficha técnica,
            // ele NÃO é extra — já está contabilizado no insumo base do produto.
            if (findProductSpecificInclude(productIncludes, canonical).isPresent()) {
                continue;
            }

            Optional<Ingredient> match = ingredientRepository
                    .findByCanonicalNameAndMerchantId(canonical, merchantId);
            if (match.isEmpty()) {
                missingIngredientNames.add(subItem.getName());
                notificationService.createMissingIngredient(subItem.getName(), canonical, merchantId);
                continue;
            }
            Ingredient ingredient = match.get();
            BigDecimal customerQuantity = BigDecimal.valueOf(subItem.getQuantity());

            BigDecimal perUnitQuantity = ingredient.getDefaultQuantity() != null
                    ? ingredient.getDefaultQuantity()
                    : BigDecimal.ONE;
            BigDecimal costPerUnit = ingredient.getCostPerUnit();

            extras.add(OrderItemExtraIngredient.builder()
                    .ingredient(ingredient)
                    .quantity(perUnitQuantity.multiply(customerQuantity))
                    .costPerUnit(costPerUnit)
                    .ingredientName(ingredient.getName())
                    .ingredientUnit(ingredient.getUnit())
                    .build());
        }
        return extras;
    }

    /**
     * Procura, entre os Includes da ficha técnica de um produto, um cujo nome normalizado
     * case com o nome canônico do subItem.
     */
    private Optional<Include> findProductSpecificInclude(List<Include> productIncludes,
                                                          String canonicalSubItemName) {
        if (productIncludes == null || productIncludes.isEmpty() || canonicalSubItemName == null) {
            return Optional.empty();
        }
        return productIncludes.stream()
                .filter(inc -> inc.getName() != null
                        && IngredientNameNormalizer.normalize(inc.getName()).equals(canonicalSubItemName))
                .findFirst();
    }
}
