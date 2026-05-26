package com.MenuBank.MenuBank.merchant;

import java.util.UUID;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(UUID id) {
        super("Comerciante com ID " + id + " não encontrado");
    }

    public MerchantNotFoundException(String message) {
        super(message);
    }
}
