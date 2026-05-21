package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryNotFoundException;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductComplementGroupRepository complementGroupRepository;
    private final UserContext userContext;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductComplementGroupRepository complementGroupRepository,
                          UserContext userContext) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.complementGroupRepository = complementGroupRepository;
        this.userContext = userContext;
    }

    public ProductResponse create(ProductRequest request) {
        UUID ownerId = userContext.getUserId();

        if (productRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateProductException("nome");
        }

        Category category = categoryRepository.findByIdAndOwnerId(request.getCategoryId(), ownerId)
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        Product product = Product.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .price(request.getPrice())
                .status(ProductStatus.ACTIVE)
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public ProductResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Product product = productRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    public Page<ProductResponse> findAll(String search, Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        String term = search == null ? "" : search;
        return productRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, term, pageable)
                .map(this::toResponse);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        UUID ownerId = userContext.getUserId();
        Product product = productRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Category category = categoryRepository.findByIdAndOwnerId(request.getCategoryId(), ownerId)
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!productRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        List<ProductComplementGroupResponse> groupResponses = complementGroupRepository
                .findByProductId(product.getId()).stream()
                .map(g -> ProductComplementGroupResponse.builder()
                        .id(g.getId())
                        .ingredientCategoryId(g.getIngredientCategory().getId())
                        .ingredientCategoryName(g.getIngredientCategory().getName())
                        .minRequired(g.getMinRequired())
                        .maxAllowed(g.getMaxAllowed())
                        .build())
                .toList();
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .status(product.getStatus())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .complementGroups(groupResponses)
                .build();
    }
}
