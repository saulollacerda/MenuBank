package com.MenuBank.MenuBank.integration.abacatepay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbacatePayWebhookBilling {
    private String id;
    private String externalId;
    private Long amount;
}
