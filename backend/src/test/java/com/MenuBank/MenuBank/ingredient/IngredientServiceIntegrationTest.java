package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.integration.IntegrationTestBase;
import com.MenuBank.MenuBank.merchant.Merchant;
import com.MenuBank.MenuBank.notification.Notification;
import com.MenuBank.MenuBank.notification.NotificationRepository;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.notification.NotificationStatus;
import com.MenuBank.MenuBank.product.Include;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IngredientService — integração com Postgres")
class IngredientServiceIntegrationTest extends IntegrationTestBase {

    @Autowired private IngredientService ingredientService;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private IncludeRepository includeRepository;
    @Autowired private com.MenuBank.MenuBank.product.IncludeService includeService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Merchant merchant;

    @BeforeEach
    void setup() {
        merchant = createMerchantAndAuthenticate();
    }

    @Test
    @DisplayName("create deve persistir ingrediente normalizando canonical name")
    void create_shouldPersistAndNormalizeCanonicalName() {
        IngredientRequest request = IngredientRequest.builder()
                .name("Leite Ninho")
                .unit("g")
                .costPerUnit(new BigDecimal("0.05"))
                .defaultQuantity(new BigDecimal("20"))
                .build();

        IngredientResponse response = ingredientService.create(request);

        Ingredient persisted = ingredientRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Leite Ninho");
        assertThat(persisted.getCanonicalName()).isEqualTo("leite ninho");
    }

    @Test
    @DisplayName("create deve rejeitar nome duplicado dentro do merchant")
    void create_shouldRejectDuplicateName() {
        IngredientRequest req = IngredientRequest.builder()
                .name("leite").unit("g").costPerUnit(new BigDecimal("0.01"))
                .defaultQuantity(BigDecimal.ONE).build();
        ingredientService.create(req);

        assertThatThrownBy(() -> ingredientService.create(req))
                .isInstanceOf(DuplicateIngredientException.class);
    }

    @Test
    @DisplayName("create deve auto-resolver notificações pendentes do mesmo canonical name")
    void create_shouldAutoResolvePendingMissingIngredientNotifications() {
        // Cria uma notificação pendente
        Notification notif = notificationService.createMissingIngredient(
                "Leite Ninho", "leite ninho", merchant.getId());
        assertThat(notif.getStatus()).isEqualTo(NotificationStatus.UNREAD);

        // Cadastra o ingrediente
        ingredientService.create(IngredientRequest.builder()
                .name("Leite Ninho").unit("g").costPerUnit(new BigDecimal("0.05"))
                .defaultQuantity(new BigDecimal("20")).build());

        Notification reloaded = notificationRepository.findById(notif.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(NotificationStatus.RESOLVED);
    }

    @Test
    @DisplayName("findByCanonicalNameAndMerchantId deve achar por nome normalizado")
    void canonicalLookup_shouldFindByNormalizedName() {
        ingredientService.create(IngredientRequest.builder()
                .name("Chocoball").unit("un").costPerUnit(new BigDecimal("0.06"))
                .defaultQuantity(BigDecimal.ONE).build());

        var found = ingredientRepository.findByCanonicalNameAndMerchantId("chocoball", merchant.getId());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("REGRESSÃO: adicionar Includes novos a um produto NÃO deve remover o Include do ingrediente (fetchUsages preservado)")
    void fetchUsages_shouldStillReturnExistingUsageAfterAddingOtherIncludesToProduct() {
        // Cenário literal do usuário:
        // - Ingrediente "Açaí Goat"
        // - Açaí 330mL com Include "Açaí Goat" 150g
        // - Açaí 500mL com Include "Açaí Goat" 240g
        // - Adicionar items novos (sacola, colher, copo) ao Açaí 500mL via IncludeService
        // - fetchUsages do Açaí Goat DEVE continuar mostrando os 2 produtos.

        IngredientResponse acaiGoat = ingredientService.create(IngredientRequest.builder()
                .name("Açaí Goat").unit("g").costPerUnit(new BigDecimal("0.05"))
                .defaultQuantity(new BigDecimal("100")).build());

        Category cat = categoryRepository.save(Category.builder()
                .merchant(merchant).name("Açaí").build());
        Product acai330 = productRepository.save(Product.builder()
                .merchant(merchant).name("Açaí 330mL").price(new BigDecimal("15"))
                .status(ProductStatus.ACTIVE).category(cat).build());
        Product acai500 = productRepository.save(Product.builder()
                .merchant(merchant).name("Açaí 500mL").price(new BigDecimal("20"))
                .status(ProductStatus.ACTIVE).category(cat).build());

        includeService.add(acai330.getId(), com.MenuBank.MenuBank.product.IncludeRequest.builder()
                .name("Açaí Goat").cost(new BigDecimal("0.05"))
                .quantity(new BigDecimal("150")).build());
        includeService.add(acai500.getId(), com.MenuBank.MenuBank.product.IncludeRequest.builder()
                .name("Açaí Goat").cost(new BigDecimal("0.05"))
                .quantity(new BigDecimal("240")).build());

        // Estado inicial: fetchUsages retorna 2
        var usagesBefore = ingredientService.fetchUsages(acaiGoat.getId());
        assertThat(usagesBefore).hasSize(2);

        // Adiciona Includes novos no Açaí 500mL (cenário do usuário)
        includeService.add(acai500.getId(), com.MenuBank.MenuBank.product.IncludeRequest.builder()
                .name("sacola").cost(new BigDecimal("0.10")).quantity(BigDecimal.ONE).build());
        includeService.add(acai500.getId(), com.MenuBank.MenuBank.product.IncludeRequest.builder()
                .name("colher").cost(new BigDecimal("0.05")).quantity(BigDecimal.ONE).build());
        includeService.add(acai500.getId(), com.MenuBank.MenuBank.product.IncludeRequest.builder()
                .name("copo").cost(new BigDecimal("0.50")).quantity(BigDecimal.ONE).build());

        // Esperado: fetchUsages ainda retorna 2 entries de Açaí Goat
        var usagesAfter = ingredientService.fetchUsages(acaiGoat.getId());
        assertThat(usagesAfter)
                .as("Açaí Goat ainda deve aparecer nos 2 produtos após adicionar items na ficha técnica")
                .hasSize(2);
        assertThat(usagesAfter).extracting("productName")
                .containsExactlyInAnyOrder("Açaí 330mL", "Açaí 500mL");

        // Também verifica direto no banco: Açaí 500mL tem 4 includes total
        var acai500Includes = includeRepository.findByProductIdAndProductMerchantId(
                acai500.getId(), merchant.getId());
        assertThat(acai500Includes).hasSize(4);
        assertThat(acai500Includes).extracting("name")
                .containsExactlyInAnyOrder("Açaí Goat", "sacola", "colher", "copo");
    }

    @Test
    @DisplayName("fetchUsages deve listar produtos cuja ficha técnica usa o ingrediente (match por nome)")
    void fetchUsages_shouldListProductsUsingIngredient() {
        IngredientResponse leite = ingredientService.create(IngredientRequest.builder()
                .name("leite ninho").unit("g").costPerUnit(new BigDecimal("0.05"))
                .defaultQuantity(new BigDecimal("20")).build());

        Category cat = categoryRepository.save(Category.builder()
                .merchant(merchant).name("Açaí").build());
        Product p1 = productRepository.save(Product.builder()
                .merchant(merchant).name("Açaí 330ml").price(new BigDecimal("15"))
                .status(ProductStatus.ACTIVE).category(cat).build());
        Product p2 = productRepository.save(Product.builder()
                .merchant(merchant).name("Açaí 500ml").price(new BigDecimal("20"))
                .status(ProductStatus.ACTIVE).category(cat).build());
        includeRepository.save(Include.builder()
                .product(p1).name("leite ninho")
                .cost(new BigDecimal("0.05")).quantity(new BigDecimal("40")).build());
        includeRepository.save(Include.builder()
                .product(p2).name("leite ninho")
                .cost(new BigDecimal("0.05")).quantity(new BigDecimal("60")).build());

        var usages = ingredientService.fetchUsages(leite.getId());

        assertThat(usages).hasSize(2);
        assertThat(usages).extracting("productName")
                .containsExactlyInAnyOrder("Açaí 330ml", "Açaí 500ml");
    }

    @Test
    @DisplayName("update deve mudar nome e recalcular canonical")
    void update_shouldRenameAndRecalculateCanonical() {
        IngredientResponse created = ingredientService.create(IngredientRequest.builder()
                .name("Velho").unit("g").costPerUnit(new BigDecimal("0.01"))
                .defaultQuantity(BigDecimal.ONE).build());

        ingredientService.update(created.getId(), IngredientRequest.builder()
                .name("Açaí Novo").unit("ml").costPerUnit(new BigDecimal("0.02"))
                .defaultQuantity(BigDecimal.TEN).build());

        Ingredient reloaded = ingredientRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Açaí Novo");
        assertThat(reloaded.getCanonicalName()).isEqualTo("acai novo");
    }

    @Test
    @DisplayName("delete deve remover ingrediente do banco")
    void delete_shouldRemoveIngredient() {
        IngredientResponse created = ingredientService.create(IngredientRequest.builder()
                .name("X").unit("g").costPerUnit(new BigDecimal("0.01"))
                .defaultQuantity(BigDecimal.ONE).build());

        ingredientService.delete(created.getId());

        assertThat(ingredientRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("findAll deve paginar por merchant filtrando por nome")
    void findAll_shouldPaginate() {
        ingredientService.create(IngredientRequest.builder()
                .name("Leite").unit("g").costPerUnit(new BigDecimal("0.05"))
                .defaultQuantity(BigDecimal.ONE).build());
        ingredientService.create(IngredientRequest.builder()
                .name("Chocoball").unit("un").costPerUnit(new BigDecimal("0.06"))
                .defaultQuantity(BigDecimal.ONE).build());

        var page = ingredientService.findAll("Leite", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Leite");
    }
}
