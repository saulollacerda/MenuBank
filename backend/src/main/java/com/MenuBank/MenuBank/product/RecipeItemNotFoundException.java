package com.MenuBank.MenuBank.product;

import java.util.UUID;

public class RecipeItemNotFoundException extends RuntimeException {

    public RecipeItemNotFoundException(UUID id) {
        super("Item da ficha técnica com ID " + id + " não encontrado");
    }
}

