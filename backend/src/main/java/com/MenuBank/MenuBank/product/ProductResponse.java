package com.MenuBank.MenuBank.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private BigDecimal estimatedCost;
    private BigDecimal margin;
    private ProductStatus status;
    private BigDecimal cmv;
}

