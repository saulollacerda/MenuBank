package com.MenuBank.MenuBank.integration.abacatepay.dto;

import lombok.Data;

@Data
public class AbacatePayCheckoutResponse {
    private AbacatePayCheckoutData data;
    private String error;
}
