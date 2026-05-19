package com.MenuBank.MenuBank.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    boolean existsByNameAndOwnerId(String name, UUID ownerId);

    Optional<PaymentMethod> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<PaymentMethod> findAllByOwnerId(UUID ownerId);

    Page<PaymentMethod> findAllByOwnerIdAndNameContainingIgnoreCase(UUID ownerId, String name, Pageable pageable);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<PaymentMethod> findByNameIgnoreCaseAndOwnerId(String name, UUID ownerId);
}
