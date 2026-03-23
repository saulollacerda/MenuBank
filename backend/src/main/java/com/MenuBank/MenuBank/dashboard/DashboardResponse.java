package com.MenuBank.MenuBank.dashboard;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private BigDecimal totalSales;
    private Long orderCount;
    private BigDecimal averageTicket;
    private BigDecimal estimatedProfit;
    private List<DailySales> salesByDay;
    private List<TopProduct> topProducts;
}

