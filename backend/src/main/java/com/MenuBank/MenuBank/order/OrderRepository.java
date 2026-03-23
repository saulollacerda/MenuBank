package com.MenuBank.MenuBank.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByDateTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, OrderStatus status);
}
