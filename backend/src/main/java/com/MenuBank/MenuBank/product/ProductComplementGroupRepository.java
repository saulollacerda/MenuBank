package com.MenuBank.MenuBank.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductComplementGroupRepository extends JpaRepository<ProductComplementGroup, UUID> {

    List<ProductComplementGroup> findByProductId(UUID productId);

    void deleteByProductId(UUID productId);
}
