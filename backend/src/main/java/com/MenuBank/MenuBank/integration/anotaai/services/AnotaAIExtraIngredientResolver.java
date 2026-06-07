package com.MenuBank.MenuBank.integration.anotaai.services;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNameNormalizer;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.integration.anotaai.AnotaAIOrderDetailResponse;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeKind;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resolve os subItems de um pedido em {@link OrderItemExtraIngredient}s.
 *
 * <p><b>Sem deduplicação:</b> cada subItem vira um {@code OrderItemExtraIngredient}
 * independente, mesmo que tenha o mesmo nome de outro subItem. Isso reflete combos
 * (e.g. Combo Casal) onde o mesmo ingrediente aparece uma vez por produto dentro
 * do pedido e deve ser contabilizado separadamente.
 *
 * <p><b>Apenas PACKAGING é autoritativo:</b> se o subItem casa com um {@link Include}
 * do tipo {@link IncludeKind#PACKAGING} (copo, embalagem...), ele já está na base do
 * produto — <b>nenhum {@code OrderItemExtraIngredient} é criado</b>, evitando
 * double-counting. Includes do tipo {@code INGREDIENT} (ou legados sem kind) NÃO entram
 * na base: são opções de personalização, então quando o cliente os pede o subItem vira
 * um extra normalmente.
 *
 * <p>Para todo subItem que não casa com um PACKAGING, o extra usa
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

        List<OrderItemExtraIngredient> extras = new ArrayList<>();
        Set<String> notifiedMissing = new java.util.HashSet<>();

        for (AnotaAIOrderDetailResponse.AnotaAISubItem subItem : subItems) {
            String rawName = subItem.getName();
            if (rawName == null || rawName.isBlank()) continue;
            String canonical = IngredientNameNormalizer.normalize(rawName);

            // Apenas PACKAGING é autoritativo: se o subItem casa com uma embalagem da
            // ficha técnica, ele já está na base do produto e NÃO vira extra. Um match
            // com INGREDIENT não pula — ingredientes só contam quando pedidos (via extra).
            if (findMatchingPackagingInclude(productIncludes, canonical).isPresent()) {
                continue;
            }

            Optional<Ingredient> match = ingredientRepository
                    .findByCanonicalNameAndMerchantId(canonical, merchantId);
            if (match.isEmpty()) {
                missingIngredientNames.add(rawName);
                if (notifiedMissing.add(canonical)) {
                    notificationService.createMissingIngredient(rawName, canonical, merchantId);
                }
                continue;
            }
            Ingredient ingredient = match.get();
            BigDecimal customerQuantity = BigDecimal.valueOf(subItem.getQuantity());
            BigDecimal perUnitQuantity = resolveQuantityForProduct(productIncludes, canonical, ingredient);
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
     * Retorna a quantidade específica do produto para o ingrediente, se houver um
     * {@link Include} não-PACKAGING com nome canônico correspondente. Caso contrário,
     * usa {@code ingredient.defaultQuantity} ou {@link BigDecimal#ONE} como fallback.
     */
    private BigDecimal resolveQuantityForProduct(List<Include> productIncludes,
                                                  String canonical,
                                                  com.MenuBank.MenuBank.ingredient.Ingredient ingredient) {
        if (productIncludes != null) {
            for (Include inc : productIncludes) {
                if (inc.getKind() == IncludeKind.PACKAGING) continue;
                if (inc.getName() == null || inc.getQuantity() == null) continue;
                if (IngredientNameNormalizer.normalize(inc.getName()).equals(canonical)) {
                    return inc.getQuantity();
                }
            }
        }
        return ingredient.getDefaultQuantity() != null ? ingredient.getDefaultQuantity() : BigDecimal.ONE;
    }

    /**
     * Procura, entre os Includes {@link IncludeKind#PACKAGING} da ficha técnica de um
     * produto, um cujo nome normalizado case com o nome canônico do subItem. Includes
     * de outros tipos (INGREDIENT ou legados sem kind) são ignorados aqui.
     */
    private Optional<Include> findMatchingPackagingInclude(List<Include> productIncludes,
                                                           String canonicalSubItemName) {
        if (productIncludes == null || productIncludes.isEmpty() || canonicalSubItemName == null) {
            return Optional.empty();
        }
        return productIncludes.stream()
                .filter(inc -> inc.getKind() == IncludeKind.PACKAGING)
                .filter(inc -> inc.getName() != null
                        && IngredientNameNormalizer.normalize(inc.getName()).equals(canonicalSubItemName))
                .findFirst();
    }
}
