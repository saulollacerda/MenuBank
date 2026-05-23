package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNameNormalizer;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderCalculations;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.payment.PaymentMethod;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.OrderCostCalculatorService;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductCostCalculator;
import com.MenuBank.MenuBank.product.ProductIngredientRepository;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import com.MenuBank.MenuBank.user.User;
import com.MenuBank.MenuBank.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AnotaAISyncService {

    private static final Logger log = LoggerFactory.getLogger(AnotaAISyncService.class);

    private final AnotaAIClient anotaAIClient;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final NotificationService notificationService;
    private final OrderCostCalculatorService orderCostCalculatorService;

    public AnotaAISyncService(AnotaAIClient anotaAIClient,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               OrderRepository orderRepository,
                               IngredientRepository ingredientRepository,
                               ProductIngredientRepository productIngredientRepository,
                               NotificationService notificationService,
                               OrderCostCalculatorService orderCostCalculatorService) {
        this.anotaAIClient = anotaAIClient;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
        this.ingredientRepository = ingredientRepository;
        this.productIngredientRepository = productIngredientRepository;
        this.notificationService = notificationService;
        this.orderCostCalculatorService = orderCostCalculatorService;
    }

    @Transactional
    public AnotaAISyncResult syncCatalog(UUID ownerId) {
        return syncCatalog(ownerId, false);
    }

    @Transactional
    public AnotaAISyncResult syncCatalog(UUID ownerId, boolean clearRecipes) {
        String apiKey = resolveApiKey(ownerId);
        AnotaAICatalogResponse catalog = anotaAIClient.getCatalog(apiKey);

        int categoriesCreated = 0, categoriesUpdated = 0;
        int productsCreated = 0, productsUpdated = 0;
        List<String> errors = new ArrayList<>();

        if (catalog == null || catalog.getCategories() == null) {
            log.warn("[Anota.AI] Catálogo vazio ou sem categorias");
            return AnotaAISyncResult.builder().errors(errors).build();
        }

        log.info("[Anota.AI] Catálogo recebido: {} categorias", catalog.getCategories().size());

        for (AnotaAICatalogResponse.AnotaAICategory remoteCategory : catalog.getCategories()) {
            // Ingredients are managed manually by the user — skip is_additional=true categories.
            if (remoteCategory.isAdditional()) continue;

            Optional<Category> existingCategoryOpt = categoryRepository
                    .findByExternalIdAndOwnerId(remoteCategory.getId(), ownerId);

            Category category;
            if (existingCategoryOpt.isPresent()) {
                category = existingCategoryOpt.get();
                category.setName(remoteCategory.getTitle());
                category = categoryRepository.save(category);
                categoriesUpdated++;
            } else {
                category = Category.builder()
                        .ownerId(ownerId)
                        .name(remoteCategory.getTitle())
                        .externalId(remoteCategory.getId())
                        .build();
                category = categoryRepository.save(category);
                categoriesCreated++;
            }

            if (remoteCategory.getItens() == null) continue;

            for (AnotaAICatalogResponse.AnotaAIItem remoteItem : remoteCategory.getItens()) {
                Optional<Product> existingProductOpt = productRepository
                        .findByExternalIdAndOwnerId(remoteItem.getId(), ownerId);

                BigDecimal price = BigDecimal.valueOf(remoteItem.getPrice());
                ProductStatus status = remoteItem.isOut() ? ProductStatus.INACTIVE : ProductStatus.ACTIVE;

                if (existingProductOpt.isPresent()) {
                    Product product = existingProductOpt.get();
                    if (clearRecipes) {
                        productIngredientRepository.deleteAllByProductIdAndProductOwnerId(product.getId(), ownerId);
                    }
                    product.setName(remoteItem.getTitle());
                    product.setPrice(price);
                    product.setStatus(status);
                    product.setCategory(category);
                    productRepository.save(product);
                    productsUpdated++;
                } else {
                    Product product = Product.builder()
                            .ownerId(ownerId)
                            .name(remoteItem.getTitle())
                            .price(price)
                            .status(status)
                            .externalId(remoteItem.getId())
                            .category(category)
                            .build();
                    productRepository.save(product);
                    productsCreated++;
                }
            }
        }

        return AnotaAISyncResult.builder()
                .categoriesCreated(categoriesCreated)
                .categoriesUpdated(categoriesUpdated)
                .productsCreated(productsCreated)
                .productsUpdated(productsUpdated)
                .errors(errors)
                .build();
    }

    @Transactional
    public AnotaAISyncResult syncOrders(UUID ownerId) {
        String apiKey = resolveApiKey(ownerId);
        AnotaAIOrderListResponse list = anotaAIClient.getOrderList(apiKey);

        int ordersImported = 0, ordersSkipped = 0;
        List<String> errors = new ArrayList<>();
        Set<String> missingIngredientNames = new LinkedHashSet<>();

        if (list == null || list.getInfo() == null || list.getInfo().getDocs() == null) {
            return AnotaAISyncResult.builder().errors(errors).build();
        }

        log.info("[Anota.AI] /ping/list retornou {} pedidos", list.getInfo().getDocs().size());

        for (AnotaAIOrderListResponse.OrderSummary summary : list.getInfo().getDocs()) {
            String externalOrderId = summary.getId();
            OrderOrigin origin = resolveOrigin(summary.getSalesChannel());

            log.info("[Anota.AI] pedido={} from='{}' salesChannel='{}' → origin={}",
                    externalOrderId, summary.getFrom(), summary.getSalesChannel(), origin);

            if (orderRepository.existsByExternalOrderIdAndOwnerId(externalOrderId, ownerId)) {
                orderRepository.findByExternalOrderIdAndOwnerId(externalOrderId, ownerId)
                        .ifPresent(existing -> {
                            if (existing.getOrigin() != origin) {
                                log.info("[Anota.AI] reclassificando pedido {} de {} → {}",
                                        externalOrderId, existing.getOrigin(), origin);
                                existing.setOrigin(origin);
                                orderRepository.save(existing);
                            }
                        });
                ordersSkipped++;
                continue;
            }

            try {
                AnotaAIOrderDetailResponse detailResponse = anotaAIClient
                        .getOrderDetail(apiKey, externalOrderId);
                if (detailResponse == null || detailResponse.getInfo() == null) {
                    log.warn("[Anota.AI] pedido {} sem dados de detalhe", externalOrderId);
                    errors.add("Pedido " + externalOrderId + " sem dados de detalhe");
                    continue;
                }
                importOrder(detailResponse.getInfo(), ownerId, origin, missingIngredientNames);
                ordersImported++;
                log.info("[Anota.AI] pedido {} importado com origin={}", externalOrderId, origin);
            } catch (RuntimeException e) {
                log.error("[Anota.AI] ERRO ao importar pedido {} (origin esperado={}): {}",
                        externalOrderId, origin, e.getMessage(), e);
                errors.add("Pedido " + externalOrderId + ": " + e.getMessage());
            }
        }

        return AnotaAISyncResult.builder()
                .ordersImported(ordersImported)
                .ordersSkipped(ordersSkipped)
                .missingIngredientNames(new ArrayList<>(missingIngredientNames))
                .errors(errors)
                .build();
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(createdAt)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (RuntimeException e) {
            log.warn("[Anota.AI] createdAt inválido '{}', usando hora atual", createdAt);
            return LocalDateTime.now();
        }
    }

    private OrderOrigin resolveOrigin(String salesChannel) {
        if (salesChannel != null && salesChannel.equalsIgnoreCase("ifood")) {
            return OrderOrigin.IFOOD;
        }
        return OrderOrigin.ANOTA_AI;
    }

    private void importOrder(AnotaAIOrderDetailResponse.OrderDetail detail, UUID ownerId,
                              OrderOrigin origin, Set<String> missingIngredientNames) {
        Customer customer = resolveCustomer(detail.getCustomer(), ownerId);
        PaymentMethod paymentMethod = resolvePaymentMethod(detail.getPayments(), ownerId);

        List<OrderItem> items = new ArrayList<>();
        if (detail.getItems() != null) {
            for (AnotaAIOrderDetailResponse.AnotaAIOrderItem remoteItem : detail.getItems()) {
                Optional<Product> productOpt = productRepository
                        .findByExternalIdAndOwnerId(remoteItem.getInternalId(), ownerId);
                if (productOpt.isEmpty()) continue;
                Product product = productOpt.get();
                BigDecimal unitCost = ProductCostCalculator.computeUnitCost(
                        productIngredientRepository.findByProductIdAndProductOwnerId(product.getId(), ownerId));
                OrderItem item = OrderItem.builder()
                        .product(product)
                        .quantity(remoteItem.getQuantity())
                        .unitPrice(BigDecimal.valueOf(remoteItem.getPrice()))
                        .unitCost(unitCost)
                        .build();
                List<OrderItemExtraIngredient> extras = buildExtraIngredients(
                        remoteItem.getSubItems(), ownerId, missingIngredientNames);
                extras.forEach(extra -> extra.setOrderItem(item));
                item.setExtraIngredients(extras);
                items.add(item);
            }
        }

        BigDecimal totalValue = BigDecimal.valueOf(detail.getTotal());
        BigDecimal deliveryFee = BigDecimal.valueOf(detail.getDeliveryFee());

        Order order = Order.builder()
                .ownerId(ownerId)
                .dateTime(parseCreatedAt(detail.getCreatedAt()))
                .customer(customer)
                .paymentMethod(paymentMethod)
                .status(OrderStatus.PENDING)
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

    /**
     * Resolves each order subItem (extra) to a local {@link Ingredient} by matching the
     * normalized name. When no match is found, registers the raw name in
     * {@code missingIngredientNames} (for the sync result) and asks
     * {@link NotificationService} to surface a notification for the user.
     */
    private List<OrderItemExtraIngredient> buildExtraIngredients(
            List<AnotaAIOrderDetailResponse.AnotaAISubItem> subItems, UUID ownerId,
            Set<String> missingIngredientNames) {
        if (subItems == null || subItems.isEmpty()) return new ArrayList<>();
        List<OrderItemExtraIngredient> extras = new ArrayList<>();
        for (AnotaAIOrderDetailResponse.AnotaAISubItem subItem : subItems) {
            String rawName = subItem.getName();
            if (rawName == null || rawName.isBlank()) continue;
            String canonical = IngredientNameNormalizer.normalize(rawName);
            Optional<Ingredient> match = ingredientRepository
                    .findByCanonicalNameAndOwnerId(canonical, ownerId);
            if (match.isEmpty()) {
                missingIngredientNames.add(rawName);
                notificationService.createMissingIngredient(rawName, canonical, ownerId);
                continue;
            }
            Ingredient ingredient = match.get();
            BigDecimal portions = BigDecimal.valueOf(subItem.getQuantity());
            BigDecimal qty = ingredient.getDefaultQuantity() != null
                    ? ingredient.getDefaultQuantity().multiply(portions)
                    : portions;
            extras.add(OrderItemExtraIngredient.builder()
                    .ingredient(ingredient)
                    .quantity(qty)
                    .costPerUnit(ingredient.getCostPerUnit())
                    .ingredientName(ingredient.getName())
                    .ingredientUnit(ingredient.getUnit())
                    .build());
        }
        return extras;
    }

    private Customer resolveCustomer(AnotaAIOrderDetailResponse.AnotaAICustomer remoteCustomer, UUID ownerId) {
        if (remoteCustomer == null) return createAnonymousCustomer(ownerId);

        String phone = remoteCustomer.getPhone();
        if (phone != null && !phone.isBlank()) {
            Optional<Customer> existing = customerRepository.findByPhoneAndOwnerId(phone, ownerId);
            if (existing.isPresent()) return existing.get();
        }

        Customer customer = Customer.builder()
                .ownerId(ownerId)
                .name(remoteCustomer.getName() != null ? remoteCustomer.getName() : "Cliente Anota.AI")
                .phone(phone)
                .externalId(remoteCustomer.getId())
                .build();
        return customerRepository.save(customer);
    }

    private Customer createAnonymousCustomer(UUID ownerId) {
        return customerRepository.save(Customer.builder().ownerId(ownerId).name("Cliente Anota.AI").build());
    }

    private PaymentMethod resolvePaymentMethod(List<AnotaAIOrderDetailResponse.AnotaAIPayment> payments,
                                                UUID ownerId) {
        if (payments == null || payments.isEmpty()) return null;
        String name = payments.get(0).getName();
        if (name == null || name.isBlank()) return null;
        return paymentMethodRepository.findByNameIgnoreCaseAndOwnerId(name, ownerId).orElse(null);
    }

    private String resolveApiKey(UUID ownerId) {
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new AnotaAIIntegrationException("Usuário não encontrado"));
        String apiKey = user.getAnotaAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AnotaAIIntegrationException("API key do Anota.AI não configurada para este usuário");
        }
        return apiKey;
    }
}
