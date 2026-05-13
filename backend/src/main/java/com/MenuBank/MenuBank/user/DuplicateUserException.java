package com.MenuBank.MenuBank.user;

public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String field) {
        super("Já existe um usuário cadastrado com este " + field);
    }
}

