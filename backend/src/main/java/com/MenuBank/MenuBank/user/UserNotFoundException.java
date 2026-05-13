package com.MenuBank.MenuBank.user;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("Usuário com ID " + id + " não encontrado");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}

