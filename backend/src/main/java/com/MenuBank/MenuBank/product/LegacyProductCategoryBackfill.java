package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
class LegacyProductCategoryBackfill implements CommandLineRunner {

    private static final String DEFAULT_NAME = "Sem categoria";
    private static final Logger log = LoggerFactory.getLogger(LegacyProductCategoryBackfill.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    LegacyProductCategoryBackfill(ProductRepository productRepository,
                                  CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Product> orphans = productRepository.findAllByCategoryIsNull();
        if (orphans.isEmpty()) {
            return;
        }

        Map<UUID, List<Product>> byOwner = orphans.stream()
                .collect(Collectors.groupingBy(Product::getOwnerId));

        byOwner.forEach((ownerId, products) -> {
            Category defaultCategory = categoryRepository
                    .findByNameAndOwnerId(DEFAULT_NAME, ownerId)
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .ownerId(ownerId)
                            .name(DEFAULT_NAME)
                            .build()));

            products.forEach(p -> p.setCategory(defaultCategory));
            productRepository.saveAll(products);

            log.info("Backfill: {} produtos legados atribuídos a '{}' para owner {}",
                    products.size(), DEFAULT_NAME, ownerId);
        });
    }
}
