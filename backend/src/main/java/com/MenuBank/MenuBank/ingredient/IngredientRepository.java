package com.MenuBank.MenuBank.ingredient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    boolean existsByNameAndMerchantId(String name, UUID merchantId);

    Optional<Ingredient> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<Ingredient> findAllByMerchantId(UUID merchantId);

    Page<Ingredient> findAllByMerchantIdAndNameContainingIgnoreCase(UUID merchantId, String name, Pageable pageable);

    boolean existsByIdAndMerchantId(UUID id, UUID merchantId);

    void deleteByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<Ingredient> findByCanonicalNameAndMerchantId(String canonicalName, UUID merchantId);
}
