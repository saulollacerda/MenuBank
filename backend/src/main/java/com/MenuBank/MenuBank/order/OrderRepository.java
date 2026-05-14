package com.MenuBank.MenuBank.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH i.extraIngredients
            WHERE o.id = :id AND o.ownerId = :ownerId
            """)
    Optional<Order> findByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            LEFT JOIN FETCH i.extraIngredients
            WHERE o.ownerId = :ownerId
            """)
    List<Order> findAllByOwnerId(@Param("ownerId") UUID ownerId);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    List<Order> findByOwnerIdAndDateTimeBetween(UUID ownerId, LocalDateTime start, LocalDateTime end);

    List<Order> findByOwnerIdAndDateTimeBetweenAndStatus(UUID ownerId, LocalDateTime start, LocalDateTime end, OrderStatus status);
}
