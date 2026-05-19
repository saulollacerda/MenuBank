package com.MenuBank.MenuBank.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String name;
    private BigDecimal price;
    private ProductStatus status;
    private UUID categoryId;
    private String categoryName;
}

