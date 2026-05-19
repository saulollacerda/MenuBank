package com.MenuBank.MenuBank.ingredient;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientCategoryResponse {

    private UUID id;
    private String name;
}
