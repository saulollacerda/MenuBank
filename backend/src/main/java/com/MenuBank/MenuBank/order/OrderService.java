package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.common.MerchantContext;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final MerchantContext merchantContext;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        IngredientRepository ingredientRepository,
                        FeeRepository feeRepository,
                        MerchantRepository merchantRepository,
                        IncludeRepository includeRepository,
                        OrderCostCalculatorService orderCostCalculatorService,
                        MerchantContext merchantContext) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.feeRepository = feeRepository;
        this.merchantRepository = merchantRepository;
        this.includeRepository = includeRepository;
        this.orderCostCalculatorService = orderCostCalculatorService;
        this.merchantContext = merchantContext;
    }

    public OrderResponse create(OrderRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        Customer customer = customerRepository.findByIdAndMerchantId(request.getCustomerId(), merchantId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Cliente com ID " + request.getCustomerId() + " não encontrado"));

        Fee fee = resolveFee(request.getFeeId(), merchantId);

        List<OrderItem> items = buildItems(request.getItems());

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

    public OrderResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();

        Order order = orderRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        return orderRepository.findPageByMerchantIdAndCustomerNameContaining(merchantId, term, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OrderResponse update(UUID id, OrderRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        Order order = orderRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Customer customer = customerRepository.findByIdAndMerchantId(request.getCustomerId(), merchantId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Cliente com ID " + request.getCustomerId() + " não encontrado"));

        Fee fee = resolveFee(request.getFeeId(), merchantId);

        List<OrderItem> newItems = buildItems(request.getItems());

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
    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!orderRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private List<OrderItem> buildItems(List<OrderItemRequest> itemRequests) {
        UUID merchantId = merchantContext.getMerchantId();
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

            List<OrderItemExtraIngredient> extraIngredients = buildExtraIngredients(itemRequest);
            extraIngredients.forEach(extra -> extra.setOrderItem(item));
            item.setExtraIngredients(extraIngredients);

            items.add(item);
        }
        return items;
    }

    private List<OrderItemExtraIngredient> buildExtraIngredients(OrderItemRequest itemRequest) {
        UUID merchantId = merchantContext.getMerchantId();
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
        List<OrderItem> items = order.getItems() != null ? order.getItems() : List.of();
        UUID orderMerchantId = order.getMerchant().getId();
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, orderMerchantId))
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
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item, UUID merchantId) {
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
        List<Include> productIncludes = includeRepository
                .findByProductIdAndProductMerchantId(item.getProduct().getId(), merchantId);
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



