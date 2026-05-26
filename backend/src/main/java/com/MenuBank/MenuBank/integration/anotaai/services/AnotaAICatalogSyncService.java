package com.MenuBank.MenuBank.integration.anotaai.services;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.integration.anotaai.AnotaAICatalogResponse;
import com.MenuBank.MenuBank.integration.anotaai.AnotaAIClient;
import com.MenuBank.MenuBank.integration.anotaai.AnotaAISyncResult;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;
import com.MenuBank.MenuBank.product.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sincroniza o catálogo (categorias + produtos) do Anota.AI com o banco do MenuBank.
 *
 * <p>Ignora categorias com {@code is_additional: true} — esses complementos são
 * gerenciados manualmente como {@link com.MenuBank.MenuBank.ingredient.Ingredient}.
 */
public class AnotaAICatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(AnotaAICatalogSyncService.class);

    private final AnotaAIClient anotaAIClient;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final IncludeRepository includeRepository;

    public AnotaAICatalogSyncService(AnotaAIClient anotaAIClient,
                                      MerchantRepository merchantRepository,
                                      CategoryRepository categoryRepository,
                                      ProductRepository productRepository,
                                      IncludeRepository includeRepository) {
        this.anotaAIClient = anotaAIClient;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.includeRepository = includeRepository;
    }

    public AnotaAISyncResult sync(UUID merchantId, String apiKey, boolean clearRecipes) {
        AnotaAICatalogResponse catalog = anotaAIClient.getCatalog(apiKey);

        int categoriesCreated = 0, categoriesUpdated = 0;
        int productsCreated = 0, productsUpdated = 0;
        List<String> errors = new ArrayList<>();

        if (catalog == null || catalog.getCategories() == null) {
            return AnotaAISyncResult.builder().errors(errors).build();
        }

        List<Category> categories = categoryRepository.findAll();

        for (AnotaAICatalogResponse.AnotaAICategory remoteCategory : catalog.getCategories()) {
            // Ingredients are managed manually by the merchant — skip is_additional=true categories.
            if (remoteCategory.isAdditional()) continue;

            Optional<Category> existingCategoryOpt = categoryRepository
                    .findByExternalIdAndMerchantId(remoteCategory.getId(), merchantId);

            Category category;
            if (existingCategoryOpt.isPresent()) {
                category = existingCategoryOpt.get();
                category.setName(remoteCategory.getTitle());
                category = categoryRepository.save(category);
                categoriesUpdated++;
            } else {
                category = Category.builder()
                        .merchant(merchantRepository.getReferenceById(merchantId))
                        .name(remoteCategory.getTitle())
                        .externalId(remoteCategory.getId())
                        .build();
                category = categoryRepository.save(category);
                categoriesCreated++;
            }

            if (remoteCategory.getItens() == null) continue;

            for (AnotaAICatalogResponse.AnotaAIItem remoteItem : remoteCategory.getItens()) {
                Optional<Product> existingProductOpt = productRepository
                        .findByExternalIdAndMerchantId(remoteItem.getId(), merchantId);

                BigDecimal price = BigDecimal.valueOf(remoteItem.getPrice());
                ProductStatus status = remoteItem.isOut() ? ProductStatus.INACTIVE : ProductStatus.ACTIVE;

                if (existingProductOpt.isPresent()) {
                    Product product = existingProductOpt.get();
                    if (clearRecipes) {
                        includeRepository.deleteAllByProductIdAndProductMerchantId(product.getId(), merchantId);
                    }
                    product.setName(remoteItem.getTitle());
                    product.setPrice(price);
                    product.setStatus(status);
                    product.setCategory(category);
                    productRepository.save(product);
                    productsUpdated++;
                } else {
                    Product product = Product.builder()
                            .merchant(merchantRepository.getReferenceById(merchantId))
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
}
