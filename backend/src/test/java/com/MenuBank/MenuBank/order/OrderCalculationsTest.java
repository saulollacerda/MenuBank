package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.payment.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderCalculations")
class OrderCalculationsTest {

    @Nested
    @DisplayName("calculateEstimatedProfit(totalValue, totalCost, paymentMethod, deliveryFee)")
    class CalculateEstimatedProfitWithDeliveryFee {

        @Test
        @DisplayName("deve subtrair deliveryFee da receita antes de calcular o lucro")
        void shouldSubtractDeliveryFeeFromRevenue() {
            BigDecimal totalValue = new BigDecimal("25.80");
            BigDecimal totalCost = new BigDecimal("5.00");
            BigDecimal deliveryFee = new BigDecimal("6.00");

            BigDecimal profit = OrderCalculations.calculateEstimatedProfit(
                    totalValue, totalCost, null, deliveryFee);

            // (25.80 - 6.00) - 5.00 - 0 (sem paymentMethod) = 14.80
            assertThat(profit).isEqualByComparingTo("14.80");
        }

        @Test
        @DisplayName("deve aplicar taxa do meio de pagamento sobre o valor total (não o líquido de entrega)")
        void shouldApplyPaymentFeeOnTotalValueIncludingDelivery() {
            BigDecimal totalValue = new BigDecimal("100.00");
            BigDecimal totalCost = new BigDecimal("30.00");
            BigDecimal deliveryFee = new BigDecimal("10.00");
            PaymentMethod pm = PaymentMethod.builder()
                    .feeRate(new BigDecimal("5.00"))
                    .build();

            BigDecimal profit = OrderCalculations.calculateEstimatedProfit(
                    totalValue, totalCost, pm, deliveryFee);

            // fee = 100 * 5% = 5.00
            // (100 - 10) - 30 - 5 = 55.00
            assertThat(profit).isEqualByComparingTo("55.00");
        }

        @Test
        @DisplayName("deve tratar deliveryFee nulo como zero")
        void shouldTreatNullDeliveryFeeAsZero() {
            BigDecimal totalValue = new BigDecimal("20.00");
            BigDecimal totalCost = new BigDecimal("5.00");

            BigDecimal profit = OrderCalculations.calculateEstimatedProfit(
                    totalValue, totalCost, null, null);

            assertThat(profit).isEqualByComparingTo("15.00");
        }
    }

    @Nested
    @DisplayName("calculateEstimatedProfit(totalValue, totalCost, paymentMethod) — overload legado")
    class CalculateEstimatedProfitLegacy {

        @Test
        @DisplayName("deve manter comportamento anterior (sem desconto de entrega)")
        void shouldKeepLegacyBehaviorWithoutDeliveryDiscount() {
            BigDecimal totalValue = new BigDecimal("25.80");
            BigDecimal totalCost = new BigDecimal("5.00");

            BigDecimal profit = OrderCalculations.calculateEstimatedProfit(
                    totalValue, totalCost, null);

            assertThat(profit).isEqualByComparingTo("20.80");
        }
    }
}
