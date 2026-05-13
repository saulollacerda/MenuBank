package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final UserContext userContext;

    public IngredientService(IngredientRepository ingredientRepository, UserContext userContext) {
        this.ingredientRepository = ingredientRepository;
        this.userContext = userContext;
    }

    public IngredientResponse create(IngredientRequest request) {
        UUID ownerId = userContext.getUserId();

        if (ingredientRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateIngredientException("nome");
        }

        Ingredient ingredient = Ingredient.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .unit(request.getUnit())
                .costPerUnit(request.getCostPerUnit())
                .defaultQuantity(request.getDefaultQuantity())
                .status(IngredientStatus.ACTIVE)
                .build();

        Ingredient saved = ingredientRepository.save(ingredient);
        return toResponse(saved);
    }

    public IngredientResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return toResponse(ingredient);
    }

    public List<IngredientResponse> findAll() {
        UUID ownerId = userContext.getUserId();
        return ingredientRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public IngredientResponse update(UUID id, IngredientRequest request) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        ingredient.setName(request.getName());
        ingredient.setUnit(request.getUnit());
        ingredient.setCostPerUnit(request.getCostPerUnit());
        ingredient.setDefaultQuantity(request.getDefaultQuantity());

        Ingredient saved = ingredientRepository.save(ingredient);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!ingredientRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new IngredientNotFoundException(id);
        }
        ingredientRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .unit(ingredient.getUnit())
                .costPerUnit(ingredient.getCostPerUnit())
                .defaultQuantity(ingredient.getDefaultQuantity())
                .status(ingredient.getStatus())
                .build();
    }
}