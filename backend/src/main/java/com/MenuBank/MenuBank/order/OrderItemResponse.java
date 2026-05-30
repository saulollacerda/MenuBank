package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.product.IncludeResponse;
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
    private BigDecimal unitCost;
    private BigDecimal totalCost;

    /**
     * Insumos da ficha técnica do produto (Includes) no momento da consulta.
     * Esses são os ingredientes base do produto, fixos por receita.
     */
    @Builder.Default
    private List<IncludeResponse> insumos = List.of();

    /**
     * Ingredientes extras adicionados pelo cliente neste pedido (subItems no Anota.AI).
     */
    @Builder.Default
    private List<OrderItemExtraIngredientResponse> extraIngredients = List.of();
}

