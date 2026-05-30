package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientCreatedEvent;
import com.MenuBank.MenuBank.ingredient.IngredientNameNormalizer;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderCalculations;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.product.OrderCostCalculatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderIngredientBackfillService {

    private static final Logger log = LoggerFactory.getLogger(OrderIngredientBackfillService.class);

    private final MerchantRepository merchantRepository;
    private final IngredientRepository ingredientRepository;
    private final OrderRepository orderRepository;
    private final AnotaAIClient anotaAIClient;
    private final OrderCostCalculatorService costCalculatorService;

    public OrderIngredientBackfillService(MerchantRepository merchantRepository,
                                          IngredientRepository ingredientRepository,
                                          OrderRepository orderRepository,
                                          AnotaAIClient anotaAIClient,
                                          OrderCostCalculatorService costCalculatorService) {
        this.merchantRepository = merchantRepository;
        this.ingredientRepository = ingredientRepository;
        this.orderRepository = orderRepository;
        this.anotaAIClient = anotaAIClient;
        this.costCalculatorService = costCalculatorService;
    }

    @Async
    @EventListener
    @Transactional
    public void onIngredientCreated(IngredientCreatedEvent event) {
        Merchant merchant = merchantRepository.findById(event.merchantId()).orElse(null);
        if (merchant == null) return;

        String apiKey = merchant.getAnotaAiApiKey();
        if (apiKey == null || apiKey.isBlank()) return;

        Ingredient ingredient = ingredientRepository.findById(event.ingredientId()).orElse(null);
        if (ingredient == null) return;

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        List<Order> orders = orderRepository.findByMerchantIdAndOriginAndDateTimeBetween(
                event.merchantId(), OrderOrigin.ANOTA_AI, startOfDay, endOfDay);

        log.info("[Backfill] ingrediente='{}' — {} pedidos hoje para verificar", ingredient.getName(), orders.size());

        for (Order order : orders) {
            try {
                backfillOrder(order, ingredient, apiKey, event.canonicalName());
            } catch (Exception e) {
                log.error("[Backfill] erro ao processar pedido externalId={}: {}",
                        order.getExternalOrderId(), e.getMessage(), e);
            }
        }
    }

    private void backfillOrder(Order order, Ingredient ingredient, String apiKey, String canonicalName) {
        if (order.getExternalOrderId() == null) return;

        AnotaAIOrderDetailResponse response = anotaAIClient.getOrderDetail(apiKey, order.getExternalOrderId());
        if (response == null || response.getInfo() == null) return;

        List<AnotaAIOrderDetailResponse.AnotaAIOrderItem> remoteItems = response.getInfo().getItems();
        if (remoteItems == null) return;

        boolean changed = false;
        for (AnotaAIOrderDetailResponse.AnotaAIOrderItem remoteItem : remoteItems) {
            OrderItem localItem = findMatchingLocalItem(order, remoteItem.getInternalId());
            if (localItem == null) continue;

            if (remoteItem.getSubItems() == null) continue;

            for (AnotaAIOrderDetailResponse.AnotaAISubItem subItem : remoteItem.getSubItems()) {
                if (subItem.getName() == null) continue;
                String subCanonical = IngredientNameNormalizer.normalize(subItem.getName());
                if (!subCanonical.equals(canonicalName)) continue;
                if (alreadyHasExtra(localItem, ingredient.getId())) continue;

                BigDecimal perUnit = ingredient.getDefaultQuantity() != null
                        ? ingredient.getDefaultQuantity()
                        : BigDecimal.ONE;
                BigDecimal qty = perUnit.multiply(BigDecimal.valueOf(subItem.getQuantity()));

                OrderItemExtraIngredient extra = OrderItemExtraIngredient.builder()
                        .orderItem(localItem)
                        .ingredient(ingredient)
                        .quantity(qty)
                        .costPerUnit(ingredient.getCostPerUnit())
                        .ingredientName(ingredient.getName())
                        .ingredientUnit(ingredient.getUnit())
                        .build();
                localItem.getExtraIngredients().add(extra);
                changed = true;
                log.info("[Backfill] extra adicionado: pedido={} item={} ingrediente='{}'",
                        order.getExternalOrderId(), localItem.getId(), ingredient.getName());
            }
        }

        if (changed) {
            order.setTotalCost(costCalculatorService.computeOrderTotalCost(order));
            order.setEstimatedProfit(OrderCalculations.calculateEstimatedProfit(order));
            orderRepository.save(order);
        }
    }

    private OrderItem findMatchingLocalItem(Order order, String remoteInternalId) {
        if (remoteInternalId == null || remoteInternalId.isBlank() || order.getItems() == null) return null;
        return order.getItems().stream()
                .filter(item -> item.getProduct() != null
                        && remoteInternalId.equals(item.getProduct().getExternalId()))
                .findFirst()
                .orElse(null);
    }

    private boolean alreadyHasExtra(OrderItem item, java.util.UUID ingredientId) {
        if (item.getExtraIngredients() == null) return false;
        return item.getExtraIngredients().stream()
                .anyMatch(e -> e.getIngredient() != null
                        && ingredientId.equals(e.getIngredient().getId()));
    }
}
