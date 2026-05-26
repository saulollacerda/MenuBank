package com.MenuBank.MenuBank.auth;

public class InactiveMerchantException extends RuntimeException {

    public InactiveMerchantException() {
        super("Conta de comerciante inativa");
    }
}
