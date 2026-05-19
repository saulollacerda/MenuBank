package com.MenuBank.MenuBank.ingredient;

public class DuplicateIngredientCategoryException extends RuntimeException {

    public DuplicateIngredientCategoryException(String field) {
        super("Já existe uma categoria de ingrediente cadastrada com este " + field);
    }
}
