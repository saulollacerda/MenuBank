package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.category.CategoryNotFoundException;
import com.MenuBank.MenuBank.category.CategoryRepository;
import com.MenuBank.MenuBank.common.MerchantContext;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantContext merchantContext;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          MerchantRepository merchantRepository,
                          MerchantContext merchantContext) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.merchantRepository = merchantRepository;
        this.merchantContext = merchantContext;
    }

    public ProductResponse create(ProductRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        if (productRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateProductException("nome");
        }

        Category category = categoryRepository.findByIdAndMerchantId(request.getCategoryId(), merchantId)
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        Product product = Product.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .name(request.getName())
                .price(request.getPrice())
                .status(ProductStatus.ACTIVE)
                .category(category)
                .build();

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public ProductResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Product product = productRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    public Page<ProductResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        return productRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, term, pageable)
                .map(this::toResponse);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Product product = productRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Category category = categoryRepository.findByIdAndMerchantId(request.getCategoryId(), merchantId)
                .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!productRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .status(product.getStatus())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .build();
    }
}
