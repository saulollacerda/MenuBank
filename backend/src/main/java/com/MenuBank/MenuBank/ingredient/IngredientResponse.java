package com.MenuBank.MenuBank.ingredient;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientResponse {

    private UUID id;
    private String name;
    private String unit;
    private BigDecimal costPerUnit;
    private BigDecimal salePrice;
    private BigDecimal defaultQuantity;
    private IngredientStatus status;
    private BigDecimal stockQuantity;
    private java.time.LocalDateTime lastReplenishedAt;
    private BigDecimal lowStockThreshold;
    private BigDecimal totalStockCost;
    private Long usageCount;
}
