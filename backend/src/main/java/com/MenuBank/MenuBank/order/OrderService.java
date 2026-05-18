package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.payment.PaymentMethod;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserContext userContext;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        IngredientRepository ingredientRepository,
                        PaymentMethodRepository paymentMethodRepository,
                        UserContext userContext) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.userContext = userContext;
    }

    public OrderResponse create(OrderRequest request) {
        UUID ownerId = userContext.getUserId();

        Customer customer = customerRepository.findByIdAndOwnerId(request.getCustomerId(), ownerId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Cliente com ID " + request.getCustomerId() + " não encontrado"));

        PaymentMethod paymentMethod = resolvePaymentMethod(request.getPaymentMethodId(), ownerId);

        List<OrderItem> items = buildItems(request.getItems());

        BigDecimal totalValue = calculateTotalValue(items);
        BigDecimal totalCost = calculateTotalCost(items);
        BigDecimal estimatedProfit = calculateEstimatedProfit(totalValue, totalCost, paymentMethod);

        Order order = Order.builder()
                .ownerId(ownerId)
                .dateTime(LocalDateTime.now())
                .customer(customer)
                .paymentMethod(paymentMethod)
                .status(OrderStatus.PAID)
                .totalValue(totalValue)
                .estimatedProfit(estimatedProfit)
                .origin(OrderOrigin.MENUBANK)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public OrderResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();

        Order order = orderRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    public List<OrderResponse> findAll() {
        UUID ownerId = userContext.getUserId();
        return orderRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse update(UUID id, OrderRequest request) {
        UUID ownerId = userContext.getUserId();

        Order order = orderRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        Customer customer = customerRepository.findByIdAndOwnerId(request.getCustomerId(), ownerId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Cliente com ID " + request.getCustomerId() + " não encontrado"));

        PaymentMethod paymentMethod = resolvePaymentMethod(request.getPaymentMethodId(), ownerId);

        List<OrderItem> newItems = buildItems(request.getItems());

        BigDecimal totalValue = calculateTotalValue(newItems);
        BigDecimal totalCost = calculateTotalCost(newItems);
        BigDecimal estimatedProfit = calculateEstimatedProfit(totalValue, totalCost, paymentMethod);

        order.setCustomer(customer);
        order.setPaymentMethod(paymentMethod);
        order.setTotalValue(totalValue);
        order.setEstimatedProfit(estimatedProfit);
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        newItems.forEach(item -> item.setOrder(order));
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }
        order.getItems().clear();
        order.getItems().addAll(newItems);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!orderRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new OrderNotFoundException(id);
        }
        orderRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private List<OrderItem> buildItems(List<OrderItemRequest> itemRequests) {
        UUID ownerId = userContext.getUserId();
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findByIdAndOwnerId(itemRequest.getProductId(), ownerId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            "Produto com ID " + itemRequest.getProductId() + " não encontrado"));

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            List<OrderItemExtraIngredient> extraIngredients = buildExtraIngredients(itemRequest);
            extraIngredients.forEach(extra -> extra.setOrderItem(item));
            item.setExtraIngredients(extraIngredients);

            items.add(item);
        }
        return items;
    }

    private List<OrderItemExtraIngredient> buildExtraIngredients(OrderItemRequest itemRequest) {
        UUID ownerId = userContext.getUserId();
        List<OrderItemExtraIngredient> extraIngredients = new ArrayList<>();
        if (itemRequest.getExtraIngredients() == null || itemRequest.getExtraIngredients().isEmpty()) {
            return extraIngredients;
        }

        for (OrderItemExtraIngredientRequest extraRequest : itemRequest.getExtraIngredients()) {
            Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(extraRequest.getIngredientId(), ownerId)
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

    private PaymentMethod resolvePaymentMethod(UUID paymentMethodId, UUID ownerId) {
        if (paymentMethodId == null) return null;
        return paymentMethodRepository.findByIdAndOwnerId(paymentMethodId, ownerId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Forma de pagamento com ID " + paymentMethodId + " não encontrada"));
    }

    private BigDecimal calculateEstimatedProfit(BigDecimal totalValue, BigDecimal totalCost,
                                                 PaymentMethod paymentMethod) {
        BigDecimal feeAmount = BigDecimal.ZERO;
        if (paymentMethod != null && paymentMethod.getFeeRate().compareTo(BigDecimal.ZERO) > 0) {
            feeAmount = totalValue.multiply(paymentMethod.getFeeRate())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        return totalValue.subtract(totalCost).subtract(feeAmount);
    }

    private BigDecimal calculateTotalValue(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalCost(List<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    BigDecimal cost = item.getProduct().getEstimatedCost();
                    if (cost == null) {
                        cost = BigDecimal.ZERO;
                    }
                    BigDecimal baseCost = cost.multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal extraCost = calculateExtraIngredientsCost(item);
                    return baseCost.add(extraCost);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateExtraIngredientsCost(OrderItem item) {
        if (item.getExtraIngredients() == null || item.getExtraIngredients().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal perUnitExtraCost = item.getExtraIngredients().stream()
                .map(extra -> extra.getQuantity().multiply(extra.getCostPerUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return perUnitExtraCost.multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(this::toItemResponse).toList()
                : List.of();

        PaymentMethod pm = order.getPaymentMethod();

        return OrderResponse.builder()
                .id(order.getId())
                .dateTime(order.getDateTime())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .status(order.getStatus())
                .totalValue(order.getTotalValue())
                .estimatedProfit(order.getEstimatedProfit())
                .paymentMethodId(pm != null ? pm.getId() : null)
                .paymentMethodName(pm != null ? pm.getName() : null)
                .feeRate(pm != null ? pm.getFeeRate() : null)
                .items(itemResponses)
                .origin(order.getOrigin())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
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

        BigDecimal baseUnitCost = item.getProduct().getEstimatedCost() != null
                ? item.getProduct().getEstimatedCost()
                : BigDecimal.ZERO;
        BigDecimal extrasPerUnit = item.getExtraIngredients() != null
                ? item.getExtraIngredients().stream()
                        .map(extra -> extra.getQuantity().multiply(extra.getCostPerUnit()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        BigDecimal unitCost = baseUnitCost.add(extrasPerUnit);
        BigDecimal totalCost = unitCost.multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .unitCost(unitCost)
                .totalCost(totalCost)
                .extraIngredients(extraResponses)
                .build();
    }
}



