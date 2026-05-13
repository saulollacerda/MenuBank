package com.MenuBank.MenuBank.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeItemRequest {

    @NotNull(message = "ID do ingrediente é obrigatório")
    private UUID ingredientId;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantidade deve ser maior que zero")
    private BigDecimal quantity;
}

