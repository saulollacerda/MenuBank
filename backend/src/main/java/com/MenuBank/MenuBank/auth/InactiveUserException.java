package com.MenuBank.MenuBank.auth;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException() {
        super("Conta de usuário inativa");
    }
}

