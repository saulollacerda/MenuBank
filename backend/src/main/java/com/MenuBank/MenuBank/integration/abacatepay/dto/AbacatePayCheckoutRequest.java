package com.MenuBank.MenuBank.integration.abacatepay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbacatePayCheckoutRequest {
    private List<AbacatePayCheckoutItem> items;
    private String method;
    private AbacatePayCustomer customer;
    private String externalId;
    private Map<String, String> metadata;
    private String returnUrl;
    private String completionUrl;
}
