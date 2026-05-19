package com.MenuBank.MenuBank.ingredient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientCategoryRepository extends JpaRepository<IngredientCategory, UUID> {

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    Optional<IngredientCategory> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<IngredientCategory> findAllByOwnerId(UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<IngredientCategory> findByExternalIdAndOwnerId(String externalId, UUID ownerId);

    List<IngredientCategory> findAllByOwnerIdAndExternalIdIn(UUID ownerId, List<String> externalIds);
}
