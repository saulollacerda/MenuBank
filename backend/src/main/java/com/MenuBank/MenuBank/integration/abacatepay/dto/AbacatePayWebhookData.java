package com.MenuBank.MenuBank.integration.abacatepay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbacatePayWebhookData {
    private AbacatePayWebhookBilling billing;
    private AbacatePayWebhookPayment payment;
}
