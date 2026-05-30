package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.fee.Fee;
import com.MenuBank.MenuBank.fee.FeeRepository;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.OrderCostCalculatorService;
import com.MenuBank.MenuBank.product.ProductCostCalculator;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.IncludeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final FeeRepository feeRepository;
    private final MerchantRepository merchantRepository;
    private final IncludeRepository includeRepository;
    private final OrderCostCalculatorService orderCostCalculatorService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        IngredientRepository ingredientRepository,
                        FeeRepository feeRepository,
                        MerchantRepository merchantRepository,
                        IncludeRepository includeRepository,
                        OrderCostCalculatorService orderCostCalculatorService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.feeRepository = feeRepository;
        this.merchantRepository = merchantRepository;
        this.includeRepository = includeRepository;
        this.orderCostCalculatorService = orderCostCalculatorService;
    }

    public OrderResponse create(UUID merchantId, OrderRequest request) {
        Customer customer = customerRepository.findByIdAndMerchantId(request.getCustomerId(), merchantId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Cliente com ID " + request.getCustomerId() + " não encontrado"));

        Fee fee = resolveFee(request.getFeeId(), merchantId);

        List<OrderItem> items = buildItems(merchantId, request.getItems());

        BigDecimal totalValue = calculateTotalValue(items);

        Order order = Order.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .dateTime(LocalDateTime.now())
                .customer(customer)
                .fee(fee)
                .status(OrderStatus.PAID)
                .totalValue(totalValue)
                .origin(OrderOrigin.MENUBANK)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        // Cálculo via service (requer order.items setado)
        BigDecimal totalCost = orderCostCalculatorService.computeOrderTotalCost(order);
        order.setTotalCost(totalCost);
        order.setEstimatedProfit(OrderCalculations.calculateEstimatedProfit(order));

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public OrderResponse findById(UUID merchantId, UUID id) {

        Order order = orderRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(UUID merchantId, String search, OrderStatus status, Pageable pageable) {
        String term = search == null ? "" : search;
        Page<Order> page = status == null
                ? orderRepository.findPageByMerchantIdAndCustomerNameContaining(merchantId, term, pageable)
                : orderRepository.findPageByMerchantIdAndStatusAndCustomerNameContaining(merchantId, status, term, pageable);

        Set<UUID> productIds = page.getContent().stream()
                .filter(o -> o.getItems() != null)
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getProduct().getId())
                .collect(Collectors.toSet());
        Map<UUID, List<Include>> includesByProduct = productIds.isEmpty() ? Map.of() :
                includeRepository.findAllByProductIdInAndProductMerchantId(productIds, merchantId)
                        .stream()
                        .collect(Collectors.groupingBy(inc -> inc.getProduct().getId()));

        return page.map(o -> toResponse(o, includesByProduct));
    }

    @Transactional(readOnly = true)
    public java.util.Map<OrderStatus, Long> statusCounts(UUID merchantId, LocalDateTime start, LocalDateTime end, String search) {
        String term = search == null ? "" : search;
        java.util.Map<OrderStatus, Long> counts = new java.util.EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : orderRepository.countByStatusForMerchant(merchantId, start, end, term)) {
            counts.put((OrderStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional
    public OrderResponse update(UUID merchantId, UUID id, OrderRequest request) {

        Order order = orderRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Customer customer = customerRepository.findByIdAndMerchantId(request.getCustomerId(), merchantId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Cliente com ID " + request.getCustomerId() + " não encontrado"));

        Fee fee = resolveFee(request.getFeeId(), merchantId);

        List<OrderItem> newItems = buildItems(merchantId, request.getItems());

        BigDecimal totalValue = calculateTotalValue(newItems);

        order.setCustomer(customer);
        order.setFee(fee);
        order.setTotalValue(totalValue);
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        newItems.forEach(item -> item.setOrder(order));
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }
        order.getItems().clear();
        order.getItems().addAll(newItems);

        // Cálculo via service (requer order.items atualizado)
        BigDecimal totalCost = orderCostCalculatorService.computeOrderTotalCost(order);
        order.setTotalCost(totalCost);
        order.setEstimatedProfit(OrderCalculations.calculateEstimatedProfit(order));

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID merchantId, UUID id) {
        if (!orderRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private List<OrderItem> buildItems(UUID merchantId, List<OrderItemRequest> itemRequests) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findByIdAndMerchantId(itemRequest.getProductId(), merchantId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            "Produto com ID " + itemRequest.getProductId() + " não encontrado"));

            BigDecimal unitCost = ProductCostCalculator.computeUnitCost(
                    includeRepository.findByProductIdAndProductMerchantId(product.getId(), merchantId));

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .unitCost(unitCost)
                    .build();

            List<OrderItemExtraIngredient> extraIngredients = buildExtraIngredients(merchantId, itemRequest);
            extraIngredients.forEach(extra -> extra.setOrderItem(item));
            item.setExtraIngredients(extraIngredients);

            items.add(item);
        }
        return items;
    }

    private List<OrderItemExtraIngredient> buildExtraIngredients(UUID merchantId, OrderItemRequest itemRequest) {
        List<OrderItemExtraIngredient> extraIngredients = new ArrayList<>();
        if (itemRequest.getExtraIngredients() == null || itemRequest.getExtraIngredients().isEmpty()) {
            return extraIngredients;
        }

        for (OrderItemExtraIngredientRequest extraRequest : itemRequest.getExtraIngredients()) {
            Ingredient ingredient = ingredientRepository.findByIdAndMerchantId(extraRequest.getIngredientId(), merchantId)
                    .orElseThrow(() -> new IngredientNotFoundException(extraRequest.getIngredientId()));

            OrderItemExtraIngredient extra = OrderItemExtraIngredient.builder()
                    .ingredient(ingredient)
                    .quantity(extraRequest.getQuantity())
                    .costPerUnit(ingredient.getCostPerUnit())
                    .ingredientName(ingredient.getName())
                    .ingredientUnit(ingredient.getUnit())
                    .build();

            extraIngredients.add(extra);
        }

        return extraIngredients;
    }

    private Fee resolveFee(UUID feeId, UUID merchantId) {
        if (feeId == null) return null;
        return feeRepository.findByIdAndMerchantId(feeId, merchantId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Taxa com ID " + feeId + " não encontrada"));
    }

    private BigDecimal calculateTotalValue(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    private OrderResponse toResponse(Order order, Map<UUID, List<Include>> includesByProduct) {
        List<OrderItem> items = order.getItems() != null ? order.getItems() : List.of();
        UUID orderMerchantId = order.getMerchant().getId();
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, orderMerchantId, includesByProduct))
                .toList();

        Fee fee = order.getFee();
        // totalCost: snapshot persistido tem prioridade; pedidos antigos (null) caem no fallback legado
        BigDecimal totalCost = order.getTotalCost() != null
                ? order.getTotalCost()
                : OrderCalculations.calculateTotalCost(items);
        // Lucro: recalcula sempre a partir dos valores do pedido para refletir a fórmula atual,
        // usando o totalCost resolvido acima (snapshot ou fallback).
        BigDecimal estimatedProfit = OrderCalculations.calculateEstimatedProfit(
                order.getTotalValue(), order.getDeliveryFee(), totalCost);

        return OrderResponse.builder()
                .id(order.getId())
                .dateTime(order.getDateTime())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .status(order.getStatus())
                .totalValue(order.getTotalValue())
                .estimatedProfit(estimatedProfit)
                .deliveryFee(order.getDeliveryFee())
                .totalCost(totalCost)
                .feeId(fee != null ? fee.getId() : null)
                .feeName(fee != null ? fee.getName() : null)
                .feeRate(fee != null ? fee.getFeeRate() : null)
                .items(itemResponses)
                .origin(order.getOrigin())
                .marginPct(computeMarginPct(estimatedProfit, order.getTotalValue()))
                .build();
    }

    private BigDecimal computeMarginPct(BigDecimal profit, BigDecimal totalValue) {
        if (totalValue == null || totalValue.signum() == 0 || profit == null) {
            return null;
        }
        return profit
                .divide(totalValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private OrderItemResponse toItemResponse(OrderItem item, UUID merchantId) {
        return toItemResponse(item, merchantId, null);
    }

    private OrderItemResponse toItemResponse(OrderItem item, UUID merchantId, Map<UUID, List<Include>> includesByProduct) {
        List<OrderItemExtraIngredientResponse> extraResponses = item.getExtraIngredients() != null
                ? item.getExtraIngredients().stream().map(extra -> {
                    BigDecimal totalCost = extra.getQuantity()
                            .multiply(extra.getCostPerUnit())
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    return OrderItemExtraIngredientResponse.builder()
                            .id(extra.getId())
                            .ingredientId(extra.getIngredient().getId())
                            .ingredientName(extra.getIngredientName())
                            .ingredientUnit(extra.getIngredientUnit())
                            .quantity(extra.getQuantity())
                            .costPerUnit(extra.getCostPerUnit())
                            .totalCost(totalCost)
                            .build();
                }).toList()
                : List.of();

        // Insumos = Includes da ficha técnica do produto (snapshot atual).
        List<Include> productIncludes = includesByProduct != null
                ? includesByProduct.getOrDefault(item.getProduct().getId(), List.of())
                : includeRepository.findByProductIdAndProductMerchantId(item.getProduct().getId(), merchantId);
        List<IncludeResponse> insumos = productIncludes.stream()
                .map(this::toIncludeResponse)
                .toList();

        // Modelo aditivo: ficha técnica (item.unitCost = mandatory base) + extras.
        BigDecimal unitCost = orderCostCalculatorService.computeItemUnitCost(item, merchantId);
        if (unitCost == null) unitCost = BigDecimal.ZERO;
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .unitCost(unitCost)
                .totalCost(totalCost)
                .insumos(insumos)
                .extraIngredients(extraResponses)
                .build();
    }

    private IncludeResponse toIncludeResponse(Include include) {
        BigDecimal cost = include.getCost() != null ? include.getCost() : BigDecimal.ZERO;
        BigDecimal quantity = include.getQuantity() != null ? include.getQuantity() : BigDecimal.ONE;
        return IncludeResponse.builder()
                .id(include.getId())
                .productId(include.getProduct().getId())
                .name(include.getName())
                .cost(cost)
                .quantity(quantity)
                .totalCost(cost.multiply(quantity))
                .build();
    }
}



