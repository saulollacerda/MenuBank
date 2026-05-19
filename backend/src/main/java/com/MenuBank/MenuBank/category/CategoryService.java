package com.MenuBank.MenuBank.category;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserContext userContext;

    public CategoryService(CategoryRepository categoryRepository, UserContext userContext) {
        this.categoryRepository = categoryRepository;
        this.userContext = userContext;
    }

    public CategoryResponse create(CategoryRequest request) {
        UUID ownerId = userContext.getUserId();

        if (categoryRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateCategoryException("nome");
        }

        Category category = Category.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    public CategoryResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Category category = categoryRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        return toResponse(category);
    }

    public Page<CategoryResponse> findAll(String search, Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        String term = search == null ? "" : search;
        return categoryRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, term, pageable)
                .map(this::toResponse);
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        UUID ownerId = userContext.getUserId();
        Category category = categoryRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setName(request.getName());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!categoryRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
