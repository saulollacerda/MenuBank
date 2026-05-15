package com.MenuBank.MenuBank.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;
}
