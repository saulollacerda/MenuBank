package com.MenuBank.MenuBank.ingredient;

import java.util.UUID;

public class IngredientCategoryNotFoundException extends RuntimeException {

    public IngredientCategoryNotFoundException(UUID id) {
        super("Categoria de ingrediente com ID " + id + " não encontrada");
    }
}
