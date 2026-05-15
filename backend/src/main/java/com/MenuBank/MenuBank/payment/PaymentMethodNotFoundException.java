package com.MenuBank.MenuBank.payment;

import java.util.UUID;

public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException(UUID id) {
        super("Forma de pagamento com ID " + id + " não encontrada");
    }
}
