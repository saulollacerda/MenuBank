package com.MenuBank.MenuBank.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeItemResponse {

    private UUID id;
    private UUID productId;
    private UUID ingredientId;
    private String ingredientName;
    private String ingredientUnit;
    private BigDecimal quantity;
    private BigDecimal costPerUnit;
    private BigDecimal totalCost;
}

