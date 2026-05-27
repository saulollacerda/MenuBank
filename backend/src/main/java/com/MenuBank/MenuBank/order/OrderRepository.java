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
            WHERE o.id = :id AND o.merchant.id = :merchantId
            """)
    Optional<Order> findByIdAndMerchantId(@Param("id") UUID id, @Param("merchantId") UUID merchantId);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.customer
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.merchant.id = :merchantId
            """)
    List<Order> findAllByMerchantId(@Param("merchantId") UUID merchantId);

    @Query(
            value = """
                    SELECT o FROM Order o
                    JOIN o.customer c
                    WHERE o.merchant.id = :merchantId
                    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(o) FROM Order o
                    JOIN o.customer c
                    WHERE o.merchant.id = :merchantId
                    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    """
    )
    Page<Order> findPageByMerchantIdAndCustomerNameContaining(
            @Param("merchantId") UUID merchantId,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByIdAndMerchantId(UUID id, UUID merchantId);

    void deleteByIdAndMerchantId(UUID id, UUID merchantId);

    List<Order> findByMerchantIdAndDateTimeBetween(UUID merchantId, LocalDateTime start, LocalDateTime end);

    List<Order> findByMerchantIdAndDateTimeBetweenAndStatus(UUID merchantId, LocalDateTime start, LocalDateTime end, OrderStatus status);

    boolean existsByExternalOrderIdAndMerchantId(String externalOrderId, UUID merchantId);

    Optional<Order> findByExternalOrderIdAndMerchantId(String externalOrderId, UUID merchantId);

    @Query(
            value = """
                SELECT COUNT(*) FROM orders o
                WHERE o.merchant_id = :merchantId
                AND o.date_time BETWEEN :start AND :end
                """,
            nativeQuery = true
    )
    Integer countByMerchantIdAndDateTimeBetween(
            @Param("merchantId") UUID merchantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(
            value = """
                    SELECT o FROM Order o
                    JOIN o.customer c
                    WHERE o.merchant.id = :merchantId
                    AND o.status = :status
                    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    """,
            countQuery = """
                    SELECT COUNT(o) FROM Order o
                    JOIN o.customer c
                    WHERE o.merchant.id = :merchantId
                    AND o.status = :status
                    AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    """
    )
    Page<Order> findPageByMerchantIdAndStatusAndCustomerNameContaining(
            @Param("merchantId") UUID merchantId,
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT o.status, COUNT(o) FROM Order o
            JOIN o.customer c
            WHERE o.merchant.id = :merchantId
            AND (:start IS NULL OR o.dateTime >= :start)
            AND (:end   IS NULL OR o.dateTime <= :end)
            AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
            GROUP BY o.status
            """)
    List<Object[]> countByStatusForMerchant(
            @Param("merchantId") UUID merchantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("search") String search);
}
