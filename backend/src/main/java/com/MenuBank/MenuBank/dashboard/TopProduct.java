package com.MenuBank.MenuBank.dashboard;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProduct {

    private String productName;
    private Long quantitySold;
}

