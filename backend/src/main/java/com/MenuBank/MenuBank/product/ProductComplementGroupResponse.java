package com.MenuBank.MenuBank.product;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductComplementGroupResponse {

    private UUID id;
    private UUID ingredientCategoryId;
    private String ingredientCategoryName;
    private int minRequired;
    private int maxAllowed;
}
