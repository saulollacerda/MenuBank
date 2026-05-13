package com.MenuBank.MenuBank.ingredient;

public class DuplicateIngredientException extends RuntimeException {

    public DuplicateIngredientException(String field) {
        super("Já existe um ingrediente cadastrado com este " + field);
    }
}

