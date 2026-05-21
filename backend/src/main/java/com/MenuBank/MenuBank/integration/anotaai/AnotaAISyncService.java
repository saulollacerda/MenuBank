package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientCategory;
import com.MenuBank.MenuBank.ingredient.IngredientCategoryRepository;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
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
import com.MenuBank.MenuBank.product.ProductComplementGroup;
import com.MenuBank.MenuBank.product.ProductComplementGroupRepository;
import com.MenuBank.MenuBank.product.ProductIngredient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final ProductComplementGroupRepository complementGroupRepository;
    private final OrderCostCalculatorService orderCostCalculatorService;

    public AnotaAISyncService(AnotaAIClient anotaAIClient,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               OrderRepository orderRepository,
                               IngredientCategoryRepository ingredientCategoryRepository,
                               IngredientRepository ingredientRepository,
                               ProductIngredientRepository productIngredientRepository,
                               ProductComplementGroupRepository complementGroupRepository,
                               OrderCostCalculatorService orderCostCalculatorService) {
        this.anotaAIClient = anotaAIClient;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.productIngredientRepository = productIngredientRepository;
        this.complementGroupRepository = complementGroupRepository;
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
        int ingredientCategoriesCreated = 0, ingredientCategoriesUpdated = 0;
        int ingredientsCreated = 0, ingredientsUpdated = 0;
        List<String> errors = new ArrayList<>();

        if (catalog == null || catalog.getCategories() == null) {
            log.warn("[Anota.AI] Catálogo vazio ou sem categorias");
            return AnotaAISyncResult.builder().errors(errors).build();
        }

        long totalCats = catalog.getCategories().size();
        long additionalCats = catalog.getCategories().stream().filter(c -> c.isAdditional()).count();
        log.info("[Anota.AI] Catálogo recebido: {} categorias totais, {} com is_additional=true",
                totalCats, additionalCats);

        // Pass 1: is_additional=true → IngredientCategory + Ingredient
        for (AnotaAICatalogResponse.AnotaAICategory remoteCategory : catalog.getCategories()) {
            if (!remoteCategory.isAdditional()) continue;
            log.debug("[Anota.AI] Processando categoria de ingrediente: {} ({})",
                    remoteCategory.getTitle(), remoteCategory.getId());

            Optional<IngredientCategory> existingOpt = ingredientCategoryRepository
                    .findByExternalIdAndOwnerId(remoteCategory.getId(), ownerId);

            IngredientCategory ingCategory;
            if (existingOpt.isPresent()) {
                ingCategory = existingOpt.get();
                ingCategory.setName(remoteCategory.getTitle());
                ingCategory = ingredientCategoryRepository.save(ingCategory);
                ingredientCategoriesUpdated++;
            } else {
                ingCategory = IngredientCategory.builder()
                        .ownerId(ownerId)
                        .name(remoteCategory.getTitle())
                        .externalId(remoteCategory.getId())
                        .build();
                ingCategory = ingredientCategoryRepository.save(ingCategory);
                ingredientCategoriesCreated++;
            }

            int itemCount = remoteCategory.getItens() == null ? 0 : remoteCategory.getItens().size();
            log.debug("[Anota.AI] Categoria adicional '{}' ({}) tem {} itens",
                    remoteCategory.getTitle(), remoteCategory.getId(), itemCount);

            if (remoteCategory.getItens() == null) continue;

            for (AnotaAICatalogResponse.AnotaAIItem remoteItem : remoteCategory.getItens()) {
                log.debug("[Anota.AI]   → item: '{}' (id={}, price={})",
                        remoteItem.getTitle(), remoteItem.getId(), remoteItem.getPrice());
                try {
                    Optional<Ingredient> existingIngOpt = ingredientRepository
                            .findByExternalIdAndOwnerId(remoteItem.getId(), ownerId);

                    BigDecimal salePrice = BigDecimal.valueOf(remoteItem.getPrice());
                    if (existingIngOpt.isPresent()) {
                        Ingredient ingredient = existingIngOpt.get();
                        ingredient.setName(remoteItem.getTitle());
                        ingredient.setCategory(ingCategory);
                        // Atualiza salePrice (cardápio pode ter mudado), mas NÃO toca em costPerUnit
                        // (cadastrado manualmente pelo restaurante)
                        ingredient.setSalePrice(salePrice);
                        ingredientRepository.save(ingredient);
                        ingredientsUpdated++;
                    } else {
                        Ingredient ingredient = Ingredient.builder()
                                .ownerId(ownerId)
                                .name(remoteItem.getTitle())
                                .unit("un")
                                .costPerUnit(BigDecimal.ZERO)  // usuário cadastra depois
                                .salePrice(salePrice)          // do Anota.AI
                                .status(IngredientStatus.ACTIVE)
                                .externalId(remoteItem.getId())
                                .category(ingCategory)
                                .build();
                        ingredientRepository.save(ingredient);
                        ingredientsCreated++;
                    }
                } catch (RuntimeException e) {
                    log.error("[Anota.AI] Erro ao salvar ingrediente {} ({}): {}",
                            remoteItem.getTitle(), remoteItem.getId(), e.getMessage(), e);
                    errors.add("Ingrediente " + remoteItem.getTitle() + ": " + e.getMessage());
                }
            }
        }

        log.info("[Anota.AI] Pass 1 concluído: {} cat. ingrediente criadas, {} atualizadas, {} ingredientes criados, {} atualizados",
                ingredientCategoriesCreated, ingredientCategoriesUpdated, ingredientsCreated, ingredientsUpdated);

        // Pass 2: is_additional=false → Category + Product
        for (AnotaAICatalogResponse.AnotaAICategory remoteCategory : catalog.getCategories()) {
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

                Product savedProduct;
                if (existingProductOpt.isPresent()) {
                    Product product = existingProductOpt.get();
                    if (clearRecipes) {
                        productIngredientRepository.deleteAllByProductIdAndProductOwnerId(product.getId(), ownerId);
                    }
                    product.setName(remoteItem.getTitle());
                    product.setPrice(price);
                    product.setStatus(status);
                    product.setCategory(category);
                    savedProduct = productRepository.save(product);
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
                    savedProduct = productRepository.save(product);
                    productsCreated++;
                }

                syncComplementGroups(remoteItem, savedProduct, ownerId);
            }
        }

        return AnotaAISyncResult.builder()
                .categoriesCreated(categoriesCreated)
                .categoriesUpdated(categoriesUpdated)
                .productsCreated(productsCreated)
                .productsUpdated(productsUpdated)
                .ingredientCategoriesCreated(ingredientCategoriesCreated)
                .ingredientCategoriesUpdated(ingredientCategoriesUpdated)
                .ingredientsCreated(ingredientsCreated)
                .ingredientsUpdated(ingredientsUpdated)
                .errors(errors)
                .build();
    }

    @Transactional
    public AnotaAISyncResult syncOrders(UUID ownerId) {
        String apiKey = resolveApiKey(ownerId);
        AnotaAIOrderListResponse list = anotaAIClient.getOrderList(apiKey);

        int ordersImported = 0, ordersSkipped = 0;
        List<String> errors = new ArrayList<>();

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
                // Pedido já importado — reclassifica origin se mudou (backfill para pedidos
                // antigos importados antes da feature de salesChannel)
                orderRepository.findByExternalOrderIdAndOwnerId(externalOrderId, ownerId)
                        .ifPresent(existing -> {
                            if (existing.getOrigin() != origin) {
                                log.info("[Anota.AI] reclassificando pedido {} de {} → {}",
                                        externalOrderId, existing.getOrigin(), origin);
                                existing.setOrigin(origin);
                                orderRepository.save(existing);
                            } else {
                                log.debug("[Anota.AI] pedido {} já está com origin correta ({})",
                                        externalOrderId, origin);
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
                importOrder(detailResponse.getInfo(), ownerId, origin);
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
                .errors(errors)
                .build();
    }

    private OrderOrigin resolveOrigin(String salesChannel) {
        if (salesChannel != null && salesChannel.equalsIgnoreCase("ifood")) {
            return OrderOrigin.IFOOD;
        }
        return OrderOrigin.ANOTA_AI;
    }

    private void importOrder(AnotaAIOrderDetailResponse.OrderDetail detail, UUID ownerId, OrderOrigin origin) {
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
                List<OrderItemExtraIngredient> extras = buildExtraIngredients(remoteItem.getSubItems(), ownerId);
                extras.forEach(extra -> extra.setOrderItem(item));
                item.setExtraIngredients(extras);
                items.add(item);
            }
        }

        BigDecimal totalValue = BigDecimal.valueOf(detail.getTotal());
        BigDecimal deliveryFee = BigDecimal.valueOf(detail.getDeliveryFee());

        Order order = Order.builder()
                .ownerId(ownerId)
                .dateTime(LocalDateTime.now())
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

        // Cálculo via service (requer order.items setado)
        BigDecimal totalCost = orderCostCalculatorService.computeOrderTotalCost(order);
        BigDecimal estimatedProfit = OrderCalculations.calculateEstimatedProfit(
                totalValue, totalCost, paymentMethod, deliveryFee);
        order.setTotalCost(totalCost);
        order.setEstimatedProfit(estimatedProfit);

        orderRepository.save(order);
    }

    private void syncComplementGroups(AnotaAICatalogResponse.AnotaAIItem remoteItem,
                                      Product product, UUID ownerId) {
        if (remoteItem.getNextSteps() == null || remoteItem.getNextSteps().isEmpty()) return;
        complementGroupRepository.deleteByProductId(product.getId());
        for (AnotaAICatalogResponse.NextStep nextStep : remoteItem.getNextSteps()) {
            ingredientCategoryRepository
                    .findByExternalIdAndOwnerId(nextStep.getCategoryId(), ownerId)
                    .ifPresent(ingCat -> complementGroupRepository.save(
                            ProductComplementGroup.builder()
                                    .product(product)
                                    .ingredientCategory(ingCat)
                                    .minRequired(nextStep.getMin())
                                    .maxAllowed(nextStep.getMax())
                                    .build()));
        }
    }

    private List<OrderItemExtraIngredient> buildExtraIngredients(
            List<AnotaAIOrderDetailResponse.AnotaAISubItem> subItems, UUID ownerId) {
        if (subItems == null || subItems.isEmpty()) return new ArrayList<>();
        List<OrderItemExtraIngredient> extras = new ArrayList<>();
        for (AnotaAIOrderDetailResponse.AnotaAISubItem subItem : subItems) {
            if (subItem.getInternalId() == null || subItem.getInternalId().isBlank()) continue;
            ingredientRepository.findByExternalIdAndOwnerId(subItem.getInternalId(), ownerId)
                    .ifPresent(ingredient -> {
                        BigDecimal qty = ingredient.getDefaultQuantity() != null
                                ? ingredient.getDefaultQuantity()
                                : BigDecimal.ONE;
                        extras.add(OrderItemExtraIngredient.builder()
                                .ingredient(ingredient)
                                .quantity(qty)
                                .costPerUnit(ingredient.getCostPerUnit())
                                .ingredientName(ingredient.getName())
                                .ingredientUnit(ingredient.getUnit())
                                .build());
                    });
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
