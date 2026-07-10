package com.MenuBank.MenuBank.integration.abacatepay.dto;

import lombok.Data;

@Data
public class AbacatePayProductResponse {
    private AbacatePayProductData data;
    private String error;
}
