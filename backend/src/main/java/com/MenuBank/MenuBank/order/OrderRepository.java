package com.MenuBank.MenuBank.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Order> findAllByOwnerId(UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    List<Order> findByOwnerIdAndDateTimeBetween(UUID ownerId, LocalDateTime start, LocalDateTime end);

    List<Order> findByOwnerIdAndDateTimeBetweenAndStatus(UUID ownerId, LocalDateTime start, LocalDateTime end, OrderStatus status);
}
