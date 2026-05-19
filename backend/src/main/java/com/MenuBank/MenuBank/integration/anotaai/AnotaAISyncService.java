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
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.payment.PaymentMethod;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductCostCalculator;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import com.MenuBank.MenuBank.product.RecipeItem;
import com.MenuBank.MenuBank.product.RecipeItemRepository;
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
    private final RecipeItemRepository recipeItemRepository;

    public AnotaAISyncService(AnotaAIClient anotaAIClient,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               OrderRepository orderRepository,
                               IngredientCategoryRepository ingredientCategoryRepository,
                               IngredientRepository ingredientRepository,
                               RecipeItemRepository recipeItemRepository) {
        this.anotaAIClient = anotaAIClient;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeItemRepository = recipeItemRepository;
    }

    @Transactional
    public AnotaAISyncResult syncCatalog(UUID ownerId) {
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

                    if (existingIngOpt.isPresent()) {
                        Ingredient ingredient = existingIngOpt.get();
                        ingredient.setName(remoteItem.getTitle());
                        ingredient.setCategory(ingCategory);
                        ingredientRepository.save(ingredient);
                        ingredientsUpdated++;
                    } else {
                        BigDecimal cost = BigDecimal.valueOf(remoteItem.getPrice());
                        Ingredient ingredient = Ingredient.builder()
                                .ownerId(ownerId)
                                .name(remoteItem.getTitle())
                                .unit("un")
                                .costPerUnit(cost)
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

                if (existingProductOpt.isPresent()) {
                    Product product = existingProductOpt.get();
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

        for (AnotaAIOrderListResponse.OrderSummary summary : list.getInfo().getDocs()) {
            String externalOrderId = summary.getId();

            if (orderRepository.existsByExternalOrderIdAndOwnerId(externalOrderId, ownerId)) {
                ordersSkipped++;
                continue;
            }

            try {
                AnotaAIOrderDetailResponse detailResponse = anotaAIClient
                        .getOrderDetail(apiKey, externalOrderId);
                if (detailResponse == null || detailResponse.getInfo() == null) {
                    errors.add("Pedido " + externalOrderId + " sem dados de detalhe");
                    continue;
                }
                importOrder(detailResponse.getInfo(), ownerId);
                ordersImported++;
            } catch (RuntimeException e) {
                errors.add("Pedido " + externalOrderId + ": " + e.getMessage());
            }
        }

        return AnotaAISyncResult.builder()
                .ordersImported(ordersImported)
                .ordersSkipped(ordersSkipped)
                .errors(errors)
                .build();
    }

    private void importOrder(AnotaAIOrderDetailResponse.OrderDetail detail, UUID ownerId) {
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
                        recipeItemRepository.findByProductIdAndProductOwnerId(product.getId(), ownerId));
                OrderItem item = OrderItem.builder()
                        .product(product)
                        .quantity(remoteItem.getQuantity())
                        .unitPrice(BigDecimal.valueOf(remoteItem.getPrice()))
                        .unitCost(unitCost)
                        .build();
                items.add(item);
            }
        }

        BigDecimal totalValue = BigDecimal.valueOf(detail.getTotal());
        BigDecimal totalCost = OrderCalculations.calculateTotalCost(items);
        BigDecimal estimatedProfit = OrderCalculations.calculateEstimatedProfit(totalValue, totalCost, paymentMethod);

        Order order = Order.builder()
                .ownerId(ownerId)
                .dateTime(LocalDateTime.now())
                .customer(customer)
                .paymentMethod(paymentMethod)
                .status(OrderStatus.PENDING)
                .totalValue(totalValue)
                .estimatedProfit(estimatedProfit)
                .origin(OrderOrigin.ANOTA_AI)
                .externalOrderId(detail.getId())
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));
        orderRepository.save(order);
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
