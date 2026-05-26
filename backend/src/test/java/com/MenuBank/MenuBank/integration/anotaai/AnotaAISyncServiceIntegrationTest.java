package com.MenuBank.MenuBank.integration.anotaai;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import com.MenuBank.MenuBank.ingredient.IngredientStatus;
import com.MenuBank.MenuBank.integration.IntegrationTestBase;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.notification.Notification;
import com.MenuBank.MenuBank.notification.NotificationRepository;
import com.MenuBank.MenuBank.notification.NotificationStatus;
import com.MenuBank.MenuBank.notification.NotificationType;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItemExtraIngredient;
import com.MenuBank.MenuBank.order.OrderOrigin;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Cobertura end-to-end de {@link AnotaAISyncService} contra Postgres real.
 *
 * <p>O único bean mockado é {@link AnotaAIClient} (não queremos hit em API externa).
 * Tudo o mais — repositórios, NotificationService, OrderCostCalculatorService —
 * roda real, ligado ao Postgres de teste.
 *
 * <p>Por que esta classe importa: o bug do {@code owner_id} estourou justamente
 * neste fluxo, no momento em que {@code NotificationService.createMissingIngredient}
 * foi acionado dentro do importOrder.
 */
@DisplayName("AnotaAISyncService — integração com Postgres")
class AnotaAISyncServiceIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private AnotaAIClient anotaAIClient;

    @Autowired private AnotaAISyncService syncService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IncludeRepository includeRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationRepository notificationRepository;

    private Merchant merchant;
    private String apiKey;

    @BeforeEach
    void setupMerchantWithApiKey() {
        merchant = createMerchant();
        apiKey = "test-key-" + merchant.getId();
        merchant.setAnotaAiApiKey(apiKey);
        merchantRepository.save(merchant);
    }

    @Test
    @DisplayName("syncOrders deve persistir pedido — Include é autoritativo: subItem que casa não vira extra")
    void syncOrders_shouldPersistFullOrderWithExtrasAndCost() {
        // Setup catalog: produto Açaí 330ml + Include leite ninho 40g
        Category category = persistCategory("Açaí");
        Product acai330 = persistProduct("Açaí 330ml", "anota-acai-330", category,
                new BigDecimal("15.00"));
        Include leiteNinhoInclude = Include.builder()
                .product(acai330)
                .name("leite ninho")
                .cost(new BigDecimal("0.05"))
                .quantity(new BigDecimal("40"))
                .build();
        includeRepository.save(leiteNinhoInclude);
        persistIngredient("leite ninho", "g",
                new BigDecimal("0.05"), new BigDecimal("20"));

        // Mock API: lista 1 pedido novo + detalhes com leite ninho qty=3
        // (mesmo nome do Include — deve ser pulado, não cria extra)
        given(anotaAIClient.getOrderList(apiKey)).willReturn(orderListWith("ord-int-1"));
        AnotaAIOrderDetailResponse detail = AnotaAIFixtures.load(
                "order_detail_acai_330_three_leite_ninho.json", AnotaAIOrderDetailResponse.class);
        detail.getInfo().setId("ord-int-1");
        detail.getInfo().getItems().get(0).setInternalId("anota-acai-330");
        given(anotaAIClient.getOrderDetail(apiKey, "ord-int-1")).willReturn(detail);

        // Act
        AnotaAISyncResult result = syncService.syncOrders(merchant.getId());

        // Assert — resultado de retorno
        assertThat(result.getOrdersImported()).isEqualTo(1);
        assertThat(result.getOrdersSkipped()).isZero();
        assertThat(result.getErrors()).isEmpty();

        // Assert — persistência real no banco
        Optional<Order> saved = orderRepository.findByExternalOrderIdAndMerchantId(
                "ord-int-1", merchant.getId());
        assertThat(saved).isPresent();
        Order order = saved.get();
        assertThat(order.getOrigin()).isEqualTo(OrderOrigin.ANOTA_AI);
        assertThat(order.getItems()).hasSize(1);

        var item = order.getItems().get(0);
        assertThat(item.getProduct().getId()).isEqualTo(acai330.getId());
        assertThat(item.getQuantity()).isEqualTo(1);
        // Include autoritativo: leite ninho está na ficha técnica, subItem é pulado
        assertThat(item.getExtraIngredients()).isEmpty();
        // totalCost = somente base (Include 40×0.05=2.00) × 1 unidade = 2.00
        assertThat(order.getTotalCost()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("syncOrders deve criar Notification de MISSING_INGREDIENT quando subItem não casa com ingrediente — END-TO-END catch do bug do owner_id")
    void syncOrders_shouldPersistMissingIngredientNotification() {
        // Setup: produto existe, mas ingrediente "leite ninho" NÃO está cadastrado
        Category category = persistCategory("Açaí");
        Product acai = persistProduct("Açaí 500ml", "anota-acai-500", category,
                new BigDecimal("20.00"));

        given(anotaAIClient.getOrderList(apiKey)).willReturn(orderListWith("ord-int-missing"));
        AnotaAIOrderDetailResponse detail = AnotaAIFixtures.load(
                "order_detail_with_subitems.json", AnotaAIOrderDetailResponse.class);
        detail.getInfo().setId("ord-int-missing");
        detail.getInfo().getItems().get(0).setInternalId("anota-acai-500");
        given(anotaAIClient.getOrderDetail(apiKey, "ord-int-missing")).willReturn(detail);

        // Act
        AnotaAISyncResult result = syncService.syncOrders(merchant.getId());

        // Assert — pedido importado, mas sem o extra
        assertThat(result.getOrdersImported()).isEqualTo(1);
        assertThat(result.getMissingIngredientNames()).contains("Açaí Premium");

        // Assert — notificação persistida no banco (este é o exato INSERT que estourava com owner_id)
        List<Notification> all = notificationRepository.findAll();
        assertThat(all).hasSize(1);
        Notification n = all.get(0);
        assertThat(n.getMerchant().getId()).isEqualTo(merchant.getId());
        assertThat(n.getType()).isEqualTo(NotificationType.MISSING_INGREDIENT);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(n.getReferenceDisplay()).isEqualTo("Açaí Premium");
        assertThat(n.getReferenceData()).isEqualTo("acai premium");
    }

    @Test
    @DisplayName("syncOrders deve agregar subItems duplicados (mesmo ingrediente em grupos diferentes) em um único extra")
    void syncOrders_shouldAggregateDuplicateSubItemsEndToEnd() {
        // Setup
        Category category = persistCategory("Açaí");
        Product acai = persistProduct("Açaí 330ml", "anota-acai-dup", category,
                new BigDecimal("15.00"));
        Ingredient leiteNinho = persistIngredient("leite ninho", "un",
                new BigDecimal("0.0533"), new BigDecimal("20"));
        Ingredient chocoball = persistIngredient("chocoball", "un",
                new BigDecimal("0.066"), new BigDecimal("20"));
        Ingredient morango = persistIngredient("morango", "un",
                new BigDecimal("0.01"), new BigDecimal("1"));

        given(anotaAIClient.getOrderList(apiKey)).willReturn(orderListWith("ord-int-dup"));
        AnotaAIOrderDetailResponse detail = AnotaAIFixtures.load(
                "order_detail_duplicate_subitems.json", AnotaAIOrderDetailResponse.class);
        detail.getInfo().setId("ord-int-dup");
        detail.getInfo().getItems().get(0).setInternalId("anota-acai-dup");
        given(anotaAIClient.getOrderDetail(apiKey, "ord-int-dup")).willReturn(detail);

        // Act
        syncService.syncOrders(merchant.getId());

        // Assert — 3 extras únicos (NÃO 5)
        Order order = orderRepository.findByExternalOrderIdAndMerchantId(
                "ord-int-dup", merchant.getId()).orElseThrow();
        var extras = order.getItems().get(0).getExtraIngredients();
        assertThat(extras).hasSize(3);

        // leite ninho: (1+1) × 20 = 40
        var leite = extras.stream()
                .filter(e -> e.getIngredient().getId().equals(leiteNinho.getId()))
                .findFirst().orElseThrow();
        assertThat(leite.getQuantity()).isEqualByComparingTo("40");

        // chocoball: (1+1) × 20 = 40
        var choco = extras.stream()
                .filter(e -> e.getIngredient().getId().equals(chocoball.getId()))
                .findFirst().orElseThrow();
        assertThat(choco.getQuantity()).isEqualByComparingTo("40");

        // morango: 1 × 1 = 1
        var mor = extras.stream()
                .filter(e -> e.getIngredient().getId().equals(morango.getId()))
                .findFirst().orElseThrow();
        assertThat(mor.getQuantity()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("syncOrders iFood — produto com internalId vazio deve casar pelo nome canônico")
    void syncOrders_iFood_shouldMatchProductByCanonicalNameWhenInternalIdIsBlank() {
        // Setup: produto cadastrado no catálogo (vindo da Anota.AI)
        Category category = persistCategory("Açaí");
        Product acai = persistProduct("Açaí 330ml", "anota-acai-330-ifood", category,
                new BigDecimal("18.49"));
        // Ingredientes cadastrados — esperados nos extras
        Ingredient leiteNinho = persistIngredient("leite ninho", "g",
                new BigDecimal("0.05"), new BigDecimal("20"));
        Ingredient chocoball = persistIngredient("chocoball", "g",
                new BigDecimal("0.06"), new BigDecimal("20"));

        // iFood manda pedido: internalId='' no item e nos subItems, mas name 'Açaí 330 ml'
        // (note o espaço extra que o iFood às vezes usa). Canonical: 'acai 330 ml' vs 'acai 330ml'.
        // O fixture já usa "Açaí 330 ml" — vamos garantir que o produto também tenha o mesmo
        // espaçamento OU que a normalização cuide disso.
        // Para esse teste, vamos usar o nome exato igual ao do fixture, sem espaço entre 330 e ml.
        acai.setName("Açaí 330 ml");
        productRepository.save(acai);

        given(anotaAIClient.getOrderList(apiKey)).willReturn(orderListIfood("ord-ifood-1"));
        AnotaAIOrderDetailResponse detail = AnotaAIFixtures.load(
                "order_detail_ifood.json", AnotaAIOrderDetailResponse.class);
        detail.getInfo().setId("ord-ifood-1");
        given(anotaAIClient.getOrderDetail(apiKey, "ord-ifood-1")).willReturn(detail);

        // Act
        AnotaAISyncResult result = syncService.syncOrders(merchant.getId());

        // Assert — pedido importado com item E extras
        assertThat(result.getOrdersImported()).isEqualTo(1);
        Order saved = orderRepository.findByExternalOrderIdAndMerchantId(
                "ord-ifood-1", merchant.getId()).orElseThrow();
        assertThat(saved.getOrigin()).isEqualTo(OrderOrigin.IFOOD);
        assertThat(saved.getItems())
                .as("Item iFood deve entrar mesmo com internalId vazio (match por nome)")
                .hasSize(1);

        var item = saved.getItems().get(0);
        assertThat(item.getProduct().getId()).isEqualTo(acai.getId());
        assertThat(item.getExtraIngredients()).hasSize(2);
        assertThat(item.getExtraIngredients()).extracting("ingredientName")
                .containsExactlyInAnyOrder("leite ninho", "chocoball");
    }

    private AnotaAIOrderListResponse orderListIfood(String orderId) {
        AnotaAIOrderListResponse response = AnotaAIFixtures.load(
                "order_list_template.json", AnotaAIOrderListResponse.class);
        var summary = response.getInfo().getDocs().get(0);
        summary.setId(orderId);
        summary.setSalesChannel("ifood");
        return response;
    }

    @Test
    @DisplayName("syncOrders deve pular pedido já importado (idempotência)")
    void syncOrders_shouldSkipAlreadyImportedOrder() {
        Category category = persistCategory("Açaí");
        Product acai = persistProduct("Açaí 500ml", "anota-idem", category,
                new BigDecimal("20.00"));

        given(anotaAIClient.getOrderList(apiKey)).willReturn(orderListWith("ord-idem"));
        AnotaAIOrderDetailResponse detail = AnotaAIFixtures.load(
                "order_detail_simple.json", AnotaAIOrderDetailResponse.class);
        detail.getInfo().setId("ord-idem");
        detail.getInfo().getItems().get(0).setInternalId("anota-idem");
        given(anotaAIClient.getOrderDetail(apiKey, "ord-idem")).willReturn(detail);

        AnotaAISyncResult first = syncService.syncOrders(merchant.getId());
        AnotaAISyncResult second = syncService.syncOrders(merchant.getId());

        assertThat(first.getOrdersImported()).isEqualTo(1);
        assertThat(second.getOrdersImported()).isZero();
        assertThat(second.getOrdersSkipped()).isEqualTo(1);
        // Apenas 1 pedido persistido — não duplicou
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private AnotaAIOrderListResponse orderListWith(String orderId) {
        AnotaAIOrderListResponse response = AnotaAIFixtures.load(
                "order_list_template.json", AnotaAIOrderListResponse.class);
        var summary = response.getInfo().getDocs().get(0);
        summary.setId(orderId);
        summary.setSalesChannel("anotaai");
        return response;
    }

    private Category persistCategory(String name) {
        return categoryRepository.save(Category.builder()
                .merchant(merchant)
                .name(name)
                .build());
    }

    private Product persistProduct(String name, String externalId, Category category, BigDecimal price) {
        return productRepository.save(Product.builder()
                .merchant(merchant)
                .name(name)
                .externalId(externalId)
                .category(category)
                .price(price)
                .status(ProductStatus.ACTIVE)
                .build());
    }

    private Ingredient persistIngredient(String name, String unit, BigDecimal cost, BigDecimal defaultQty) {
        return ingredientRepository.save(Ingredient.builder()
                .merchant(merchant)
                .name(name)
                .canonicalName(name) // já normalizado nos casos do teste
                .unit(unit)
                .costPerUnit(cost)
                .defaultQuantity(defaultQty)
                .status(IngredientStatus.ACTIVE)
                .build());
    }
}
