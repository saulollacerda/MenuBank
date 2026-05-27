package com.MenuBank.MenuBank.category;

import com.MenuBank.MenuBank.common.MerchantContext;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantContext merchantContext;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           MerchantRepository merchantRepository,
                           MerchantContext merchantContext,
                           ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.merchantRepository = merchantRepository;
        this.merchantContext = merchantContext;
        this.productRepository = productRepository;
    }

    public CategoryResponse create(CategoryRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        if (categoryRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateCategoryException("nome");
        }

        Category category = Category.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .name(request.getName())
                .build();

        Category saved = categoryRepository.save(category);
        return toResponseWithCount(saved, merchantId);
    }

    public CategoryResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Category category = categoryRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return toResponseWithCount(category, merchantId);
    }

    public Page<CategoryResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        Page<Category> page = categoryRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, term, pageable);
        List<UUID> ids = page.getContent().stream().map(Category::getId).toList();
        Map<UUID, Long> counts = ids.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> productRepository.countByCategoryIdAndMerchantId(id, merchantId)));
        return page.map(c -> toResponse(c, counts.getOrDefault(c.getId(), 0L)));
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Category category = categoryRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setName(request.getName());

        Category saved = categoryRepository.save(category);
        return toResponseWithCount(saved, merchantId);
    }

    @Transactional
    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!categoryRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private CategoryResponse toResponseWithCount(Category category, UUID merchantId) {
        long count = productRepository.countByCategoryIdAndMerchantId(category.getId(), merchantId);
        return toResponse(category, count);
    }

    private CategoryResponse toResponse(Category category, long productCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .productCount(productCount)
                .build();
    }
}
