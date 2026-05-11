package com.MenuBank.MenuBank.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, UUID> {

    List<RecipeItem> findByProductIdAndProductOwnerId(UUID productId, UUID ownerId);

    Optional<RecipeItem> findByIdAndProductIdAndProductOwnerId(UUID id, UUID productId, UUID ownerId);

    void deleteByIdAndProductIdAndProductOwnerId(UUID id, UUID productId, UUID ownerId);

    boolean existsByProductIdAndIngredientId(UUID productId, UUID ingredientId);
}

