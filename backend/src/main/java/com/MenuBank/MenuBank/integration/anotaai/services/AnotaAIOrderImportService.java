package com.MenuBank.MenuBank.integration.anotaai.services;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.fee.Fee;
import com.MenuBank.MenuBank.fee.FeeRepository;
import com.MenuBank.MenuBank.integration.anotaai.AnotaAIOrderDetailResponse;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderCalculations;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.OrderCostCalculatorService;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductCostCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Importa um pedido individual do Anota.AI, persistindo {@link Order}, {@link OrderItem}s
 * e {@link OrderItemExtraIngredient}s. Delega as resoluções de cliente, produto e extras
 * para os resolvers especializados.
 */
public class AnotaAIOrderImportService {

    private static final Logger log = LoggerFactory.getLogger(AnotaAIOrderImportService.class);

    // Anota.AI/iFood enviam createdAt em UTC. Convertemos sempre para o fuso de
    // Brasília — não dependemos do timezone do servidor (em prod/Railway é UTC).
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;
    private final FeeRepository feeRepository;
    private final IncludeRepository includeRepository;
    private final OrderCostCalculatorService orderCostCalculatorService;
    private final AnotaAICustomerResolver customerResolver;
    private final AnotaAIProductResolver productResolver;
    private final AnotaAIExtraIngredientResolver extraIngredientResolver;

    public AnotaAIOrderImportService(MerchantRepository merchantRepository,
                                      OrderRepository orderRepository,
                                      FeeRepository feeRepository,
                                      IncludeRepository includeRepository,
                                      OrderCostCalculatorService orderCostCalculatorService,
                                      AnotaAICustomerResolver customerResolver,
                                      AnotaAIProductResolver productResolver,
                                      AnotaAIExtraIngredientResolver extraIngredientResolver) {
        this.merchantRepository = merchantRepository;
        this.orderRepository = orderRepository;
        this.feeRepository = feeRepository;
        this.includeRepository = includeRepository;
        this.orderCostCalculatorService = orderCostCalculatorService;
        this.customerResolver = customerResolver;
        this.productResolver = productResolver;
        this.extraIngredientResolver = extraIngredientResolver;
    }

    public void importOrder(AnotaAIOrderDetailResponse.OrderDetail detail,
                             UUID merchantId,
                             OrderOrigin origin,
                             Set<String> missingIngredientNames,
                             Runnable onCatalogSyncRequested,
                             boolean[] catalogSynced) {
        Customer customer = customerResolver.resolve(detail.getCustomer(), merchantId);
        Fee fee = resolveFee(detail.getPayments(), merchantId);

        List<OrderItem> items = new ArrayList<>();
        if (detail.getItems() != null) {
            for (AnotaAIOrderDetailResponse.AnotaAIOrderItem remoteItem : detail.getItems()) {
                Optional<Product> productOpt = productResolver.resolve(remoteItem, merchantId);

                if (productOpt.isEmpty()) {
                    if (!catalogSynced[0]) {
                        log.info("[Anota.AI] produto '{}' / '{}' não encontrado — sincronizando catálogo e tentando novamente",
                                remoteItem.getInternalId(), remoteItem.getName());
                        onCatalogSyncRequested.run();
                        catalogSynced[0] = true;
                        productOpt = productResolver.resolve(remoteItem, merchantId);
                    }
                    if (productOpt.isEmpty()) {
                        log.warn("[Anota.AI] pulando item — produto não encontrado: internalId='{}' name='{}'",
                                remoteItem.getInternalId(), remoteItem.getName());
                        continue;
                    }
                }

                Product product = productOpt.get();
                List<Include> productIncludes =
                        includeRepository.findByProductIdAndProductMerchantId(product.getId(), merchantId);
                BigDecimal unitCost = ProductCostCalculator.computeOrderBaseCost(productIncludes);
                OrderItem item = OrderItem.builder()
                        .product(product)
                        .quantity(remoteItem.getQuantity())
                        .unitPrice(BigDecimal.valueOf(remoteItem.getPrice()))
                        .unitCost(unitCost)
                        .build();
                List<OrderItemExtraIngredient> extras = extraIngredientResolver.resolve(
                        remoteItem.getSubItems(), productIncludes, merchantId, missingIngredientNames);
                extras.forEach(extra -> extra.setOrderItem(item));
                item.setExtraIngredients(extras);
                items.add(item);
            }
        }

        BigDecimal deliveryFee = BigDecimal.valueOf(detail.getDeliveryFee());
        // detail.total da Anota.AI já inclui a taxa de entrega (item.total + deliveryFee = detail.total).
        // Usar diretamente — somar deliveryFee inflaria o totalValue em duplicidade.
        BigDecimal totalValue = BigDecimal.valueOf(detail.getTotal());

        Order order = Order.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .dateTime(parseCreatedAt(detail.getCreatedAt()))
                .customer(customer)
                .fee(fee)
                .status(OrderStatus.PAID)
                .totalValue(totalValue)
                .deliveryFee(deliveryFee)
                .origin(origin)
                .externalOrderId(detail.getId())
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        BigDecimal totalCost = orderCostCalculatorService.computeOrderTotalCost(order);
        order.setTotalCost(totalCost);
        order.setEstimatedProfit(OrderCalculations.calculateEstimatedProfit(order));

        orderRepository.save(order);
    }

    LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return LocalDateTime.now(BRAZIL_ZONE);
        }
        try {
            return OffsetDateTime.parse(createdAt)
                    .atZoneSameInstant(BRAZIL_ZONE)
                    .toLocalDateTime();
        } catch (RuntimeException e) {
            log.warn("[Anota.AI] createdAt inválido '{}', usando hora atual", createdAt);
            return LocalDateTime.now(BRAZIL_ZONE);
        }
    }

    private Fee resolveFee(List<AnotaAIOrderDetailResponse.AnotaAIPayment> payments, UUID merchantId) {
        if (payments == null || payments.isEmpty()) return null;
        String name = payments.get(0).getName();
        if (name == null || name.isBlank()) return null;
        return feeRepository.findByNameIgnoreCaseAndMerchantId(name, merchantId).orElse(null);
    }
}
