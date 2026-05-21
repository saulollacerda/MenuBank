package com.MenuBank.MenuBank.product;

import java.util.UUID;

public class ProductIngredientNotFoundException extends RuntimeException {

    public ProductIngredientNotFoundException(UUID id) {
        super("Ingrediente do produto com ID " + id + " não encontrado");
    }
}
