package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Calcula o custo total estimado de um {@link Order} no modelo aditivo:
 * <pre>
 *   item.unitCost   = ficha técnica (mandatory base) + sum(extras.quantity × extras.costPerUnit)
 *   item.totalCost  = item.unitCost × item.quantity
 *   order.totalCost = sum(item.totalCost)
 * </pre>
 * O {@code OrderItem.unitCost} é gravado por {@link ProductCostCalculator} no momento
 * em que o pedido é criado/importado — este service não consulta {@code ProductIngredient}.
 */
@Service
public class OrderCostCalculatorService {

    public OrderCostCalculatorService() {
    }

    public BigDecimal computeOrderTotalCost(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return order.getItems().stream()
                .map(item -> computeItemTotalCost(item, order.getMerchant().getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Custo unitário do item = base da ficha técnica ({@code item.unitCost}) +
     * soma dos extras (quantity × costPerUnit). NÃO multiplica pela quantidade do item.
     * Parâmetro {@code merchantId} reservado para evoluções futuras (snapshot externo).
     */
    public BigDecimal computeItemUnitCost(OrderItem item, UUID merchantId) {
        BigDecimal baseCost = item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO;
        BigDecimal extrasPerUnit = BigDecimal.ZERO;
        if (item.getExtraIngredients() != null) {
            for (OrderItemExtraIngredient extra : item.getExtraIngredients()) {
                BigDecimal q = extra.getQuantity() != null ? extra.getQuantity() : BigDecimal.ZERO;
                BigDecimal c = extra.getCostPerUnit() != null ? extra.getCostPerUnit() : BigDecimal.ZERO;
                extrasPerUnit = extrasPerUnit.add(q.multiply(c));
            }
        }
        return baseCost.add(extrasPerUnit);
    }

    private BigDecimal computeItemTotalCost(OrderItem item, UUID merchantId) {
        return computeItemUnitCost(item, merchantId)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
