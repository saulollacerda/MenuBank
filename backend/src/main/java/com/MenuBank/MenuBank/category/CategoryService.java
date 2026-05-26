package com.MenuBank.MenuBank.category;

import com.MenuBank.MenuBank.common.MerchantContext;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantContext merchantContext;

    public CategoryService(CategoryRepository categoryRepository,
                           MerchantRepository merchantRepository,
                           MerchantContext merchantContext) {
        this.categoryRepository = categoryRepository;
        this.merchantRepository = merchantRepository;
        this.merchantContext = merchantContext;
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
        return toResponse(saved);
    }

    public CategoryResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Category category = categoryRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return toResponse(category);
    }

    public Page<CategoryResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        return categoryRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, term, pageable)
                .map(this::toResponse);
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Category category = categoryRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setName(request.getName());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!categoryRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
