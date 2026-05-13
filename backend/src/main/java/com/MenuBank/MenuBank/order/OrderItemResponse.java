package com.MenuBank.MenuBank.order;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    @Builder.Default
    private List<OrderItemExtraIngredientResponse> extraIngredients = List.of();
}

