package com.MenuBank.MenuBank.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, UUID> {

    List<RecipeItem> findByProductId(UUID productId);

    void deleteByProductIdAndId(UUID productId, UUID id);

    boolean existsByProductIdAndIngredientId(UUID productId, UUID ingredientId);
}

