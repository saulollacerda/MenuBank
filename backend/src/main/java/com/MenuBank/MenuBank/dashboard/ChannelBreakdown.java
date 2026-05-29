package com.MenuBank.MenuBank.dashboard;

import com.MenuBank.MenuBank.order.OrderOrigin;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBreakdown {

    private OrderOrigin origin;
    private Long orderCount;
    private BigDecimal pct;
}
