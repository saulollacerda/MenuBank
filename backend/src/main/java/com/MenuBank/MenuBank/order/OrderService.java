package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNameNormalizer;
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
import com.MenuBank.MenuBank.product.IncludeKind;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.IncludeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    // Pedidos manuais usam a hora de Brasília — não dependemos do timezone do servidor
    // (em prod/Railway é UTC), que adiantaria o horário em 3h.
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final FeeRepository feeRepository;
    private final MerchantRepository merchantRepository;
    private final IncludeRepository includeRepository;
    private final OrderCostCalculatorService orderCostCalculatorService;
    private final OrderFichaService orderFichaService;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository,
                        IngredientRepository ingredientRepository,
                        FeeRepository feeRepository,
                        MerchantRepository merchantRepository,
                        IncludeRepository includeRepository,
                        OrderCostCalculatorService orderCostCalculatorService,
                        OrderFichaService orderFichaService) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.feeRepository = feeRepository;
        this.merchantRepository = merchantRepository;
        this.includeRepository = includeRepository;
        this.orderCostCalculatorService = orderCostCalculatorService;
        this.orderFichaService = orderFichaService;
    }

    @Transactional
    public OrderResponse create(UUID merchantId, OrderRequest request) {
        Customer customer = resolveCustomer(merchantId, request);

        Fee fee = resolveFee(request.getFeeId(), merchantId);

        List<OrderItem> items = buildItems(merchantId, request.getItems());

        BigDecimal totalValue = calculateTotalValue(items);

        Order order = Order.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .dateTime(LocalDateTime.now(BRAZIL_ZONE))
                .customer(customer)
                .fee(fee)
                .status(OrderStatus.PAID)
                .totalValue(totalValue)
                .origin(request.getOrigin() != null ? request.getOrigin() : OrderOrigin.MENUBANK)
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        // Ficha do pedido: insumos cobrados UMA vez, fora do laço dos itens.
        attachOrderFicha(order, merchantId);

        // Cálculo via service (requer order.items e order.orderFicha setados)
        BigDecimal totalCost = orderCostCalculatorService.computeOrderTotalCost(order);
        order.setTotalCost(totalCost);
        order.setEstimatedProfit(OrderCalculations.calculateEstimatedProfit(order));

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    /**
     * Copia a ficha do pedido do lojista para o pedido (snapshot). Sem ficha configurada
     * a lista fica vazia e o custo não muda — no-op para quem não usa a funcionalidade.
     */
    private void attachOrderFicha(Order order, UUID merchantId) {
        List<OrderFichaIngredient> ficha = orderFichaService.buildSnapshot(merchantId);
        ficha.forEach(line -> line.setOrder(order));
        order.setOrderFicha(ficha);
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

        Customer customer = resolveCustomer(merchantId, request);

        Fee fee = resolveFee(request.getFeeId(), merchantId);

        List<OrderItem> newItems = buildItems(merchantId, request.getItems());

        BigDecimal totalValue = calculateTotalValue(newItems);

        order.setCustomer(customer);
        order.setFee(fee);
        order.setTotalValue(totalValue);
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
        }
        if (request.getOrigin() != null) {
            order.setOrigin(request.getOrigin());
        }
        newItems.forEach(item -> item.setOrder(order));
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }
        order.getItems().clear();
        order.getItems().addAll(newItems);

        // A ficha do pedido NÃO é re-snapshotada aqui: o pedido conserva a ficha com que
        // foi criado. Editar um pedido antigo não pode importar a ficha de hoje.

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

            // Pedido manual: a ficha técnica completa acompanha o produto por padrão;
            // o operador desmarca os insumos que ficaram de fora (excludedIncludeIds).
            Set<UUID> excludedIncludeIds = itemRequest.getExcludedIncludeIds() != null
                    ? new java.util.HashSet<>(itemRequest.getExcludedIncludeIds())
                    : new java.util.HashSet<>();
            BigDecimal unitCost = ProductCostCalculator.computeSelectedCost(
                    includeRepository.findByProductIdAndProductMerchantId(product.getId(), merchantId),
                    excludedIncludeIds);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .unitCost(unitCost)
                    .excludedIncludeIds(excludedIncludeIds)
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

    /**
     * Resolve o cliente do pedido. Com {@code customerId}, busca o cliente existente
     * (404 quando não encontrado). Com apenas {@code customerName} — fluxo rápido da
     * UI — reutiliza o primeiro cliente do lojista cujo nome case no formato canônico
     * (sem caixa/acentos) ou cria um novo somente com o nome.
     */
    private Customer resolveCustomer(UUID merchantId, OrderRequest request) {
        if (request.getCustomerId() != null) {
            return customerRepository.findByIdAndMerchantId(request.getCustomerId(), merchantId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            "Cliente com ID " + request.getCustomerId() + " não encontrado"));
        }

        String canonicalName = IngredientNameNormalizer.normalize(request.getCustomerName());
        return customerRepository.findAllByMerchantId(merchantId).stream()
                .filter(c -> IngredientNameNormalizer.normalize(c.getName()).equals(canonicalName))
                .findFirst()
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .merchant(merchantRepository.getReferenceById(merchantId))
                        .name(request.getCustomerName().trim())
                        .build()));
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
        Set<String> registeredCanonicalNames = registeredCanonicalNamesFor(items, orderMerchantId);
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, orderMerchantId, includesByProduct, order.getOrigin(),
                        registeredCanonicalNames))
                .toList();

        List<OrderFichaIngredientResponse> orderFichaResponses = toOrderFichaResponses(order);
        BigDecimal orderFichaCost = orderCostCalculatorService.computeOrderFichaCost(order);
        if (orderFichaCost == null) orderFichaCost = BigDecimal.ZERO;

        Fee fee = order.getFee();
        // totalCost: snapshot persistido tem prioridade; pedidos antigos (null) caem no fallback legado.
        // O fallback soma a ficha do pedido para não perder essa parcela caso o snapshot de
        // totalCost falte — pedidos pré-V17 não têm ficha, então continuam idênticos.
        BigDecimal totalCost = order.getTotalCost() != null
                ? order.getTotalCost()
                : OrderCalculations.calculateTotalCost(items).add(orderFichaCost);
        // Lucro: recalcula sempre a partir dos valores do pedido para refletir a fórmula atual,
        // usando o totalCost resolvido acima (snapshot ou fallback) e a taxa de meio de pagamento.
        BigDecimal estimatedProfit = OrderCalculations.calculateEstimatedProfit(
                order.getTotalValue(), order.getDeliveryFee(), order.getServiceFee(), totalCost,
                fee != null ? fee.getFeeRate() : null);

        return OrderResponse.builder()
                .id(order.getId())
                .dateTime(order.getDateTime())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .status(order.getStatus())
                .totalValue(order.getTotalValue())
                .estimatedProfit(estimatedProfit)
                .deliveryFee(order.getDeliveryFee())
                .serviceFee(order.getServiceFee())
                .totalCost(totalCost)
                .feeId(fee != null ? fee.getId() : null)
                .feeName(fee != null ? fee.getName() : null)
                .feeRate(fee != null ? fee.getFeeRate() : null)
                .items(itemResponses)
                .origin(order.getOrigin())
                .marginPct(computeMarginPct(estimatedProfit, order.getTotalValue(),
                        order.getDeliveryFee(), order.getServiceFee()))
                .orderFicha(orderFichaResponses)
                .orderFichaCost(orderFichaCost)
                .build();
    }

    /**
     * Linhas da ficha do pedido gravadas NO pedido. Sempre do snapshot — nunca da
     * configuração atual do lojista —, para que o detalhe mostre o que foi cobrado.
     */
    private List<OrderFichaIngredientResponse> toOrderFichaResponses(Order order) {
        if (order.getOrderFicha() == null) {
            return List.of();
        }
        return order.getOrderFicha().stream()
                .map(line -> {
                    BigDecimal quantity = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
                    BigDecimal costPerUnit = line.getCostPerUnit() != null ? line.getCostPerUnit() : BigDecimal.ZERO;
                    return OrderFichaIngredientResponse.builder()
                            .id(line.getId())
                            .ingredientId(line.getIngredient() != null ? line.getIngredient().getId() : null)
                            .ingredientName(line.getIngredientName())
                            .ingredientUnit(line.getIngredientUnit())
                            .quantity(quantity)
                            .costPerUnit(costPerUnit)
                            .totalCost(quantity.multiply(costPerUnit))
                            .build();
                })
                .toList();
    }

    /**
     * Margem (%) do pedido sobre o subtotal dos produtos ({@code totalValue − deliveryFee −
     * serviceFee}), mesma base usada no cálculo do lucro. A taxa de entrega e a taxa de serviço
     * são excluídas do denominador porque já estão excluídas do numerador.
     */
    private BigDecimal computeMarginPct(BigDecimal profit, BigDecimal totalValue,
                                        BigDecimal deliveryFee, BigDecimal serviceFee) {
        BigDecimal base = OrderCalculations.calculateProductsSubtotal(totalValue, deliveryFee, serviceFee);
        if (base.signum() == 0 || profit == null) {
            return null;
        }
        return profit
                .divide(base, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Nomes canônicos dos subItems não-casados que JÁ existem como ingrediente do merchant.
     * Uma única consulta cobre todos os itens do pedido — usada para derivar quais botões de
     * "cadastrar ingrediente" já não são necessários (o ingrediente foi criado). Vazio quando
     * o pedido não tem subItems não-casados, evitando a consulta.
     */
    private Set<String> registeredCanonicalNamesFor(List<OrderItem> items, UUID merchantId) {
        Set<String> canonicalNames = items.stream()
                .filter(item -> item.getUnmatchedSubItems() != null)
                .flatMap(item -> item.getUnmatchedSubItems().stream())
                .map(OrderItemUnmatchedSubItem::getCanonicalName)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (canonicalNames.isEmpty()) {
            return Set.of();
        }
        return new java.util.HashSet<>(
                ingredientRepository.findExistingCanonicalNames(merchantId, canonicalNames));
    }

    private OrderItemResponse toItemResponse(OrderItem item, UUID merchantId, Map<UUID, List<Include>> includesByProduct,
                                             OrderOrigin origin, Set<String> registeredCanonicalNames) {
        List<OrderItemExtraIngredientResponse> extraResponses = item.getExtraIngredients() != null
                ? item.getExtraIngredients().stream().map(extra -> {
                    BigDecimal totalCost = extra.getQuantity()
                            .multiply(extra.getCostPerUnit())
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    // Preço pago é repassado como foi gravado, sem multiplicar por
                    // item.quantity: o total do pedido já embute os valores dos subItems.
                    return OrderItemExtraIngredientResponse.builder()
                            .id(extra.getId())
                            .ingredientId(extra.getIngredient().getId())
                            .ingredientName(extra.getIngredientName())
                            .ingredientUnit(extra.getIngredientUnit())
                            .quantity(extra.getQuantity())
                            .costPerUnit(extra.getCostPerUnit())
                            .totalCost(totalCost)
                            .salePricePerUnit(extra.getSalePricePerUnit())
                            .salePriceTotal(extra.getSalePriceTotal())
                            .build();
                }).toList()
                : List.of();

        // Insumos = Includes da ficha técnica do produto (snapshot atual).
        // Pedido manual (MENUBANK/legado sem origem): PACKAGING + legados sem kind menos
        // os insumos desmarcados pelo operador (excludedIncludeIds). INGREDIENT nunca é
        // puxado — só conta quando pedido como extra.
        // Pedido importado (AnotaAI/iFood): apenas PACKAGING — ingredientes escolhidos
        // chegam via extraIngredients (subItems).
        List<Include> productIncludes = includesByProduct != null
                ? includesByProduct.getOrDefault(item.getProduct().getId(), List.of())
                : includeRepository.findByProductIdAndProductMerchantId(item.getProduct().getId(), merchantId);
        Set<UUID> excludedIncludeIds = item.getExcludedIncludeIds() != null
                ? item.getExcludedIncludeIds()
                : Set.of();
        boolean manualOrder = origin == null || origin == OrderOrigin.MENUBANK;
        List<IncludeResponse> insumos = productIncludes.stream()
                .filter(inc -> manualOrder
                        ? inc.getKind() != IncludeKind.INGREDIENT
                        : inc.getKind() == IncludeKind.PACKAGING)
                .filter(inc -> inc.getId() == null || !excludedIncludeIds.contains(inc.getId()))
                .map(this::toIncludeResponse)
                .toList();

        // SubItems não-casados: expostos apenas enquanto não houver um ingrediente com o
        // mesmo nome canônico. Assim que o lojista cadastra o ingrediente, o registro é
        // filtrado e o botão de cadastro some no próximo carregamento do pedido.
        List<OrderItemUnmatchedSubItemResponse> unmatchedResponses = item.getUnmatchedSubItems() != null
                ? item.getUnmatchedSubItems().stream()
                    .filter(u -> u.getCanonicalName() == null
                            || !registeredCanonicalNames.contains(u.getCanonicalName()))
                    .map(u -> OrderItemUnmatchedSubItemResponse.builder()
                            .id(u.getId())
                            .rawName(u.getRawName())
                            .quantity(u.getQuantity())
                            .salePricePerUnit(u.getSalePricePerUnit())
                            .salePriceTotal(u.getSalePriceTotal())
                            .build())
                    .toList()
                : List.of();

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
                .unmatchedSubItems(unmatchedResponses)
                .excludedIncludeIds(List.copyOf(excludedIncludeIds))
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



