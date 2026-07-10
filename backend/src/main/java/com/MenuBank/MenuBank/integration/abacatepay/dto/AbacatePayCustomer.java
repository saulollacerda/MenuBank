package com.MenuBank.MenuBank.integration.abacatepay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbacatePayCustomer {
    private String name;
    private String cellphone;
    private String email;
    private String taxId;
}
