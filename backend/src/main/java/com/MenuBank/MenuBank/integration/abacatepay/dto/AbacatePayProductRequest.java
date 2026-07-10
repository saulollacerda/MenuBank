package com.MenuBank.MenuBank.integration.abacatepay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbacatePayProductRequest {
    private String externalId;
    private String name;
    private String description;
    private long price;
    private String currency;
}
