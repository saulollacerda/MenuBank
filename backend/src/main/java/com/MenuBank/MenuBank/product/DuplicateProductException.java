package com.MenuBank.MenuBank.product;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(String field) {
        super("Já existe um produto cadastrado com este " + field);
    }
}

