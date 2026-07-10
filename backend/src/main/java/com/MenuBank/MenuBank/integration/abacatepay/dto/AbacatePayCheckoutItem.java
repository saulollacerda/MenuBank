package com.MenuBank.MenuBank.integration.abacatepay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbacatePayCheckoutItem {
    private String id;
    private int quantity;
}
