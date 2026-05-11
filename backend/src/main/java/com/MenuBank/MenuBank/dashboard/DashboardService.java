package com.MenuBank.MenuBank.dashboard;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.order.Order;
import com.MenuBank.MenuBank.order.OrderItem;
import com.MenuBank.MenuBank.order.OrderRepository;
import com.MenuBank.MenuBank.order.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserContext userContext;

    public DashboardService(OrderRepository orderRepository, UserContext userContext) {
        this.orderRepository = orderRepository;
        this.userContext = userContext;
    }

    public DashboardResponse getDashboard(LocalDate startDate, LocalDate endDate) {
        UUID ownerId = userContext.getUserId();

        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        List<Order> paidOrders = orderRepository.findByOwnerIdAndDateTimeBetweenAndStatus(
                ownerId, startDateTime, endDateTime, OrderStatus.PAID);

        BigDecimal totalSales = calculateTotalSales(paidOrders);
        long orderCount = paidOrders.size();
        BigDecimal averageTicket = calculateAverageTicket(totalSales, orderCount);
        BigDecimal estimatedProfit = calculateEstimatedProfit(paidOrders);
        List<DailySales> salesByDay = calculateSalesByDay(paidOrders);
        List<TopProduct> topProducts = calculateTopProducts(paidOrders);

        return DashboardResponse.builder()
                .totalSales(totalSales)
                .orderCount(orderCount)
                .averageTicket(averageTicket)
                .estimatedProfit(estimatedProfit)
                .salesByDay(salesByDay)
                .topProducts(topProducts)
                .build();
    }

    private BigDecimal calculateTotalSales(List<Order> orders) {
        return orders.stream()
                .map(Order::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAverageTicket(BigDecimal totalSales, long orderCount) {
        if (orderCount == 0) {
            return BigDecimal.ZERO;
        }
        return totalSales.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateEstimatedProfit(List<Order> orders) {
        return orders.stream()
                .map(Order::getEstimatedProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DailySales> calculateSalesByDay(List<Order> orders) {
        Map<LocalDate, BigDecimal> salesMap = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getDateTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalValue, BigDecimal::add)
                ));

        return salesMap.entrySet().stream()
                .map(entry -> DailySales.builder()
                        .date(entry.getKey())
                        .total(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(DailySales::getDate))
                .toList();
    }

    private List<TopProduct> calculateTopProducts(List<Order> orders) {
        Map<String, Long> productQuantities = orders.stream()
                .filter(order -> order.getItems() != null)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getName(),
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        return productQuantities.entrySet().stream()
                .map(entry -> TopProduct.builder()
                        .productName(entry.getKey())
                        .quantitySold(entry.getValue())
                        .build())
                .sorted(Comparator.comparingLong(TopProduct::getQuantitySold).reversed())
                .limit(5)
                .toList();
    }
}
