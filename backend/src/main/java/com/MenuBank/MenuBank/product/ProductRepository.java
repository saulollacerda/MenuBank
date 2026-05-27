package com.MenuBank.MenuBank.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByNameAndMerchantId(String name, UUID merchantId);

    Optional<Product> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<Product> findAllByMerchantId(UUID merchantId);

    Page<Product> findAllByMerchantIdAndNameContainingIgnoreCase(UUID merchantId, String name, Pageable pageable);

    boolean existsByIdAndMerchantId(UUID id, UUID merchantId);

    void deleteByIdAndMerchantId(UUID id, UUID merchantId);

    List<Product> findAllByCategoryIsNull();

    Optional<Product> findByExternalIdAndMerchantId(String externalId, UUID merchantId);

    long countByCategoryIdAndMerchantId(UUID categoryId, UUID merchantId);
}
