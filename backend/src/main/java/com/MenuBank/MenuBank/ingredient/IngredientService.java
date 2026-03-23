package com.MenuBank.MenuBank.ingredient;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    public IngredientResponse create(IngredientRequest request) {
        if (ingredientRepository.existsByName(request.getName())) {
            throw new DuplicateIngredientException("nome");
        }

        Ingredient ingredient = Ingredient.builder()
                .name(request.getName())
                .unit(request.getUnit())
                .costPerUnit(request.getCostPerUnit())
                .status(IngredientStatus.ACTIVE)
                .build();

        Ingredient saved = ingredientRepository.save(ingredient);
        return toResponse(saved);
    }

    public IngredientResponse findById(UUID id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return toResponse(ingredient);
    }

    public List<IngredientResponse> findAll() {
        return ingredientRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public IngredientResponse update(UUID id, IngredientRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        ingredient.setName(request.getName());
        ingredient.setUnit(request.getUnit());
        ingredient.setCostPerUnit(request.getCostPerUnit());

        Ingredient saved = ingredientRepository.save(ingredient);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        if (!ingredientRepository.existsById(id)) {
            throw new IngredientNotFoundException(id);
        }
        ingredientRepository.deleteById(id);
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .unit(ingredient.getUnit())
                .costPerUnit(ingredient.getCostPerUnit())
                .status(ingredient.getStatus())
                .build();
    }
}
