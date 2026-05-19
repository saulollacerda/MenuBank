package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class RecipeItemService {

    private final RecipeItemRepository recipeItemRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final UserContext userContext;

    public RecipeItemService(RecipeItemRepository recipeItemRepository,
                             ProductRepository productRepository,
                             IngredientRepository ingredientRepository,
                             UserContext userContext) {
        this.recipeItemRepository = recipeItemRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.userContext = userContext;
    }

    @Transactional
    public RecipeItemResponse addRecipeItem(UUID productId, RecipeItemRequest request) {
        UUID ownerId = userContext.getUserId();

        Product product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(request.getIngredientId(), ownerId)
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
        UUID ownerId = userContext.getUserId();

        if (!productRepository.existsByIdAndOwnerId(productId, ownerId)) {
            throw new ProductNotFoundException(productId);
        }
        return recipeItemRepository.findByProductIdAndProductOwnerId(productId, ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecipeItemResponse update(UUID productId, UUID recipeItemId, RecipeItemRequest request) {
        UUID ownerId = userContext.getUserId();

        RecipeItem recipeItem = recipeItemRepository.findByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId)
                .orElseThrow(() -> new RecipeItemNotFoundException(recipeItemId));

        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(request.getIngredientId(), ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(request.getIngredientId()));

        recipeItem.setIngredient(ingredient);
        recipeItem.setQuantity(request.getQuantity());

        RecipeItem saved = recipeItemRepository.save(recipeItem);

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID productId, UUID recipeItemId) {
        UUID ownerId = userContext.getUserId();

        if (recipeItemRepository.findByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId).isEmpty()) {
            throw new RecipeItemNotFoundException(recipeItemId);
        }
        recipeItemRepository.deleteByIdAndProductIdAndProductOwnerId(recipeItemId, productId, ownerId);
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
