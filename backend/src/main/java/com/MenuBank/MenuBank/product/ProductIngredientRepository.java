package com.MenuBank.MenuBank.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, UUID> {

    List<ProductIngredient> findByProductIdAndProductOwnerId(UUID productId, UUID ownerId);

    Optional<ProductIngredient> findByIdAndProductIdAndProductOwnerId(UUID id, UUID productId, UUID ownerId);

    Optional<ProductIngredient> findByProductIdAndIngredientIdAndProductOwnerId(
            UUID productId, UUID ingredientId, UUID ownerId);

    @Modifying
    @Transactional
    void deleteByIdAndProductIdAndProductOwnerId(UUID id, UUID productId, UUID ownerId);

    @Modifying
    @Transactional
    long deleteAllByProductIdAndProductOwnerId(UUID productId, UUID ownerId);

    boolean existsByProductIdAndIngredientId(UUID productId, UUID ingredientId);
}
