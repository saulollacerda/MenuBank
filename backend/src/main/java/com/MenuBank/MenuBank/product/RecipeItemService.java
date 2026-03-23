package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class RecipeItemService {

    private final RecipeItemRepository recipeItemRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    public RecipeItemService(RecipeItemRepository recipeItemRepository,
                             ProductRepository productRepository,
                             IngredientRepository ingredientRepository) {
        this.recipeItemRepository = recipeItemRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public RecipeItemResponse addRecipeItem(UUID productId, RecipeItemRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new IngredientNotFoundException(request.getIngredientId()));

        RecipeItem recipeItem = RecipeItem.builder()
                .product(product)
                .ingredient(ingredient)
                .quantity(request.getQuantity())
                .build();

        RecipeItem saved = recipeItemRepository.save(recipeItem);
        return toResponse(saved);
    }

    public List<RecipeItemResponse> findByProductId(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return recipeItemRepository.findByProductId(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    public RecipeItemResponse update(UUID productId, UUID recipeItemId, RecipeItemRequest request) {
        RecipeItem recipeItem = recipeItemRepository.findById(recipeItemId)
                .orElseThrow(() -> new RecipeItemNotFoundException(recipeItemId));

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new IngredientNotFoundException(request.getIngredientId()));

        recipeItem.setIngredient(ingredient);
        recipeItem.setQuantity(request.getQuantity());

        RecipeItem saved = recipeItemRepository.save(recipeItem);
        return toResponse(saved);
    }

    public void delete(UUID productId, UUID recipeItemId) {
        if (!recipeItemRepository.findById(recipeItemId).isPresent()) {
            throw new RecipeItemNotFoundException(recipeItemId);
        }
        recipeItemRepository.deleteById(recipeItemId);
    }

    private RecipeItemResponse toResponse(RecipeItem recipeItem) {
        Ingredient ingredient = recipeItem.getIngredient();
        BigDecimal totalCost = recipeItem.getQuantity().multiply(ingredient.getCostPerUnit());

        return RecipeItemResponse.builder()
                .id(recipeItem.getId())
                .productId(recipeItem.getProduct().getId())
                .ingredientId(ingredient.getId())
                .ingredientName(ingredient.getName())
                .ingredientUnit(ingredient.getUnit())
                .quantity(recipeItem.getQuantity())
                .costPerUnit(ingredient.getCostPerUnit())
                .totalCost(totalCost)
                .build();
    }
}
