package com.MenuBank.MenuBank.payment;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    private UUID id;
    private String name;
    private BigDecimal feeRate;
}
