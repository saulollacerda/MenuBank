package com.MenuBank.MenuBank.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncludeRepository extends JpaRepository<Include, UUID> {

    List<Include> findByProductIdAndProductMerchantId(UUID productId, UUID merchantId);

    Optional<Include> findByIdAndProductIdAndProductMerchantId(UUID id, UUID productId, UUID merchantId);

    @Modifying
    @Transactional
    void deleteByIdAndProductIdAndProductMerchantId(UUID id, UUID productId, UUID merchantId);

    @Modifying
    @Transactional
    long deleteAllByProductIdAndProductMerchantId(UUID productId, UUID merchantId);

    /**
     * Busca todos os includes cujo {@code name} bate (case-insensitive) com o nome dado,
     * dentro do tenant do merchant. Usado para descobrir em quais produtos um ingrediente
     * cadastrado e referenciado nas fichas tecnicas (matching por nome).
     */
    List<Include> findByNameIgnoreCaseAndProductMerchantId(String name, UUID merchantId);
}
