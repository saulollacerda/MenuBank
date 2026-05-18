package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.customer.CustomerRepository;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import com.MenuBank.MenuBank.payment.PaymentMethod;
import com.MenuBank.MenuBank.payment.PaymentMethodRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import com.MenuBank.MenuBank.user.User;
import com.MenuBank.MenuBank.user.UserRepository;
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

    private final AnotaAIClient anotaAIClient;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;

    public AnotaAISyncService(AnotaAIClient anotaAIClient,
                               UserRepository userRepository,
                               CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               PaymentMethodRepository paymentMethodRepository,
                               OrderRepository orderRepository) {
        this.anotaAIClient = anotaAIClient;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public AnotaAISyncResult syncCatalog(UUID ownerId) {
        String apiKey = resolveApiKey(ownerId);
        AnotaAICatalogResponse catalog = anotaAIClient.getCatalog(apiKey);

        int categoriesCreated = 0;
        int categoriesUpdated = 0;
        int productsCreated = 0;
        int productsUpdated = 0;
        List<String> errors = new ArrayList<>();

        if (catalog == null || catalog.getCategories() == null) {
            return AnotaAISyncResult.builder().errors(errors).build();
        }

        for (AnotaAICatalogResponse.AnotaAICategory remoteCategory : catalog.getCategories()) {
            if (remoteCategory.isAdditional()) {
                continue;
            }

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

            if (remoteCategory.getItens() == null) {
                continue;
            }

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
                .errors(errors)
                .build();
    }

    @Transactional
    public AnotaAISyncResult syncOrders(UUID ownerId) {
        String apiKey = resolveApiKey(ownerId);
        AnotaAIOrderListResponse list = anotaAIClient.getOrderList(apiKey);

        int ordersImported = 0;
        int ordersSkipped = 0;
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
                if (productOpt.isEmpty()) {
                    continue;
                }
                OrderItem item = OrderItem.builder()
                        .product(productOpt.get())
                        .quantity(remoteItem.getQuantity())
                        .unitPrice(BigDecimal.valueOf(remoteItem.getPrice()))
                        .build();
                items.add(item);
            }
        }

        BigDecimal totalValue = BigDecimal.valueOf(detail.getTotal());

        Order order = Order.builder()
                .ownerId(ownerId)
                .dateTime(LocalDateTime.now())
                .customer(customer)
                .paymentMethod(paymentMethod)
                .status(OrderStatus.PENDING)
                .totalValue(totalValue)
                .estimatedProfit(BigDecimal.ZERO)
                .origin(OrderOrigin.ANOTA_AI)
                .externalOrderId(detail.getId())
                .items(items)
                .build();

        items.forEach(item -> item.setOrder(order));

        orderRepository.save(order);
    }

    private Customer resolveCustomer(AnotaAIOrderDetailResponse.AnotaAICustomer remoteCustomer, UUID ownerId) {
        if (remoteCustomer == null) {
            return createAnonymousCustomer(ownerId);
        }

        String phone = remoteCustomer.getPhone();
        if (phone != null && !phone.isBlank()) {
            Optional<Customer> existing = customerRepository.findByPhoneAndOwnerId(phone, ownerId);
            if (existing.isPresent()) {
                return existing.get();
            }
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
        Customer customer = Customer.builder()
                .ownerId(ownerId)
                .name("Cliente Anota.AI")
                .build();
        return customerRepository.save(customer);
    }

    private PaymentMethod resolvePaymentMethod(List<AnotaAIOrderDetailResponse.AnotaAIPayment> payments,
                                                UUID ownerId) {
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        String name = payments.get(0).getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return paymentMethodRepository.findByNameIgnoreCaseAndOwnerId(name, ownerId).orElse(null);
    }

    private String resolveApiKey(UUID ownerId) {
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new AnotaAIIntegrationException("Usuário não encontrado"));
        String apiKey = user.getAnotaAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AnotaAIIntegrationException(
                    "API key do Anota.AI não configurada para este usuário");
        }
        return apiKey;
    }
}
