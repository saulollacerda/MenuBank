package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientCategoryRepository ingredientCategoryRepository;
    private final UserContext userContext;

    public IngredientService(IngredientRepository ingredientRepository,
                             IngredientCategoryRepository ingredientCategoryRepository,
                             UserContext userContext) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientCategoryRepository = ingredientCategoryRepository;
        this.userContext = userContext;
    }

    public IngredientResponse create(IngredientRequest request) {
        UUID ownerId = userContext.getUserId();

        if (ingredientRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateIngredientException("nome");
        }

        IngredientCategory category = resolveCategory(request.getIngredientCategoryId(), ownerId);

        Ingredient ingredient = Ingredient.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .unit(request.getUnit())
                .costPerUnit(request.getCostPerUnit())
                .defaultQuantity(request.getDefaultQuantity())
                .status(IngredientStatus.ACTIVE)
                .category(category)
                .build();

        return toResponse(ingredientRepository.save(ingredient));
    }

    public IngredientResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return toResponse(ingredient);
    }

    public Page<IngredientResponse> findAll(String search, Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        String term = search == null ? "" : search;
        return ingredientRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, term, pageable)
                .map(this::toResponse);
    }

    public IngredientResponse update(UUID id, IngredientRequest request) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        IngredientCategory category = resolveCategory(request.getIngredientCategoryId(), ownerId);

        ingredient.setName(request.getName());
        ingredient.setUnit(request.getUnit());
        ingredient.setCostPerUnit(request.getCostPerUnit());
        ingredient.setDefaultQuantity(request.getDefaultQuantity());
        ingredient.setCategory(category);

        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientResponse updateCost(UUID id, IngredientCostRequest request) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        ingredient.setCostPerUnit(request.getCostPerUnit());
        if (request.getDefaultQuantity() != null) {
            ingredient.setDefaultQuantity(request.getDefaultQuantity());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            ingredient.setUnit(request.getUnit());
        }
        // NÃO toca em salePrice — esse campo é gerenciado pelo sync do Anota.AI

        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!ingredientRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new IngredientNotFoundException(id);
        }
        ingredientRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private IngredientCategory resolveCategory(UUID categoryId, UUID ownerId) {
        if (categoryId == null) {
            return null;
        }
        return ingredientCategoryRepository.findByIdAndOwnerId(categoryId, ownerId)
                .orElseThrow(() -> new IngredientCategoryNotFoundException(categoryId));
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        IngredientCategory cat = ingredient.getCategory();
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .unit(ingredient.getUnit())
                .costPerUnit(ingredient.getCostPerUnit())
                .salePrice(ingredient.getSalePrice())
                .defaultQuantity(ingredient.getDefaultQuantity())
                .status(ingredient.getStatus())
                .ingredientCategoryId(cat != null ? cat.getId() : null)
                .ingredientCategoryName(cat != null ? cat.getName() : null)
                .externalId(ingredient.getExternalId())
                .build();
    }
}