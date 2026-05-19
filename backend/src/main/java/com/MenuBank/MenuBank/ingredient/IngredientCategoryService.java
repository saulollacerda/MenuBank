package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IngredientCategoryService {

    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final UserContext userContext;

    public IngredientCategoryService(IngredientCategoryRepository ingredientCategoryRepository,
                                     UserContext userContext) {
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.userContext = userContext;
    }

    public IngredientCategoryResponse create(IngredientCategoryRequest request) {
        UUID ownerId = userContext.getUserId();

        if (ingredientCategoryRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateIngredientCategoryException("nome");
        }

        IngredientCategory category = IngredientCategory.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .build();

        return toResponse(ingredientCategoryRepository.save(category));
    }

    public IngredientCategoryResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        IngredientCategory category = ingredientCategoryRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientCategoryNotFoundException(id));
        return toResponse(category);
    }

    public List<IngredientCategoryResponse> findAll() {
        UUID ownerId = userContext.getUserId();
        return ingredientCategoryRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public IngredientCategoryResponse update(UUID id, IngredientCategoryRequest request) {
        UUID ownerId = userContext.getUserId();
        IngredientCategory category = ingredientCategoryRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientCategoryNotFoundException(id));

        category.setName(request.getName());
        return toResponse(ingredientCategoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!ingredientCategoryRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new IngredientCategoryNotFoundException(id);
        }
        ingredientCategoryRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private IngredientCategoryResponse toResponse(IngredientCategory category) {
        return IngredientCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
