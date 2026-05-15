package com.MenuBank.MenuBank.payment;

public class DuplicatePaymentMethodException extends RuntimeException {

    public DuplicatePaymentMethodException(String field) {
        super("Já existe uma forma de pagamento com este " + field);
    }
}
