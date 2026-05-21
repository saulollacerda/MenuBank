package com.MenuBank.MenuBank.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            WHERE o.id = :id AND o.ownerId = :ownerId
            """)
    Optional<Order> findByIdAndOwnerId(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.ownerId = :ownerId
            """)
    List<Order> findAllByOwnerId(@Param("ownerId") UUID ownerId);

    @Query(
            value = """
                    SELECT o FROM Order o
                    JOIN o.customer c
                    WHERE o.ownerId = :ownerId
                    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(o) FROM Order o
                    JOIN o.customer c
                    WHERE o.ownerId = :ownerId
                    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    """
    )
    Page<Order> findPageByOwnerIdAndCustomerNameContaining(
            @Param("ownerId") UUID ownerId,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    void deleteByIdAndOwnerId(UUID id, UUID ownerId);

    List<Order> findByOwnerIdAndDateTimeBetween(UUID ownerId, LocalDateTime start, LocalDateTime end);

    List<Order> findByOwnerIdAndDateTimeBetweenAndStatus(UUID ownerId, LocalDateTime start, LocalDateTime end, OrderStatus status);

    boolean existsByExternalOrderIdAndOwnerId(String externalOrderId, UUID ownerId);

    Optional<Order> findByExternalOrderIdAndOwnerId(String externalOrderId, UUID ownerId);
}
