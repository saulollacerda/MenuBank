package com.MenuBank.MenuBank.integration.ifood.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodOrderDetailResponse {

    private String id;
    private String displayId;
    private String orderType;
    private String orderTiming;
    private String salesChannel;
    private String category;
    private String createdAt;
    private String preparationStartDateTime;

    // iFood documentation names this field "isTest" but some payloads serialize it as "test"
    @JsonAlias("isTest")
    private boolean test;

    private String extraInfo;

    private MerchantInfo merchant;
    private CustomerInfo customer;
    private List<Item> items;
    private Total total;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MerchantInfo {
        private String id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerInfo {
        private String id;
        private String name;
        private String documentNumber;
        private Phone phone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Phone {
        private String number;
        private String localizer;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Integer index;
        private String id;
        private String uniqueId;
        private String externalCode;
        private String ean;
        private String name;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal optionsPrice;
        private BigDecimal totalPrice;
        private String observations;
        private List<Option> options;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Option {
        private Integer index;
        private String id;
        private String name;
        private String groupName;
        private String externalCode;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal addition;
        private BigDecimal price;
        // TODO: options[].customizations[] (3rd level) intentionally not mapped in v1
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Total {
        private BigDecimal subTotal;
        private BigDecimal deliveryFee;
        private BigDecimal additionalFees;
        private BigDecimal benefits;
        private BigDecimal orderAmount;
    }
}
