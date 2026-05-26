package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.fee.Fee;
import com.MenuBank.MenuBank.merchant.Merchant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Merchant merchant;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = true)
    private Fee fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalValue;

    @Column(nullable = false)
    private BigDecimal estimatedProfit;

    @Column(name = "delivery_fee", precision = 19, scale = 4)
    private BigDecimal deliveryFee;

    @Column(name = "total_cost", precision = 19, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin")
    private OrderOrigin origin;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items;
}

