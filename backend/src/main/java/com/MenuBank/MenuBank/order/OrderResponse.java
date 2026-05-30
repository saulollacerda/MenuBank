package com.MenuBank.MenuBank.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private LocalDateTime dateTime;
    private UUID customerId;
    private String customerName;
    private OrderStatus status;
    private BigDecimal totalValue;
    private BigDecimal estimatedProfit;
    private BigDecimal deliveryFee;
    private BigDecimal totalCost;
    private UUID feeId;
    private String feeName;
    private BigDecimal feeRate;
    private List<OrderItemResponse> items;
    private OrderOrigin origin;
    private BigDecimal marginPct;
}

