package com.MenuBank.MenuBank.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "ID do cliente é obrigatório")
    private UUID customerId;

    @NotEmpty(message = "Pedido deve ter pelo menos um item")
    @Valid
    private List<OrderItemRequest> items;

    private OrderStatus status;
}

