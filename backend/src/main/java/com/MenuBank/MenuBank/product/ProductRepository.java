package com.MenuBank.MenuBank.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    Optional<Product> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Product> findAllByOwnerId(UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    List<Product> findAllByCategoryIsNull();

    Optional<Product> findByExternalIdAndOwnerId(String externalId, UUID ownerId);
}
