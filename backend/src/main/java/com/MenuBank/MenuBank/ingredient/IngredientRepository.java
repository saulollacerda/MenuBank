package com.MenuBank.MenuBank.ingredient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    Optional<Ingredient> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Ingredient> findAllByOwnerId(UUID ownerId);

    Page<Ingredient> findAllByOwnerIdAndNameContainingIgnoreCase(UUID ownerId, String name, Pageable pageable);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Ingredient> findByExternalIdAndOwnerId(String externalId, UUID ownerId);

    List<Ingredient> findAllByCategoryAndOwnerId(IngredientCategory category, UUID ownerId);
}
