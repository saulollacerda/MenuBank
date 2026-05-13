package com.MenuBank.MenuBank.category;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String field) {
        super("Já existe uma categoria cadastrada com este " + field);
    }
}

