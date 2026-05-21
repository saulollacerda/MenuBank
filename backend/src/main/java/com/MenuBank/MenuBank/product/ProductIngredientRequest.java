package com.MenuBank.MenuBank.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductIngredientRequest {

    @NotNull(message = "ID do ingrediente é obrigatório")
    private UUID ingredientId;

    @NotNull(message = "Gramatura é obrigatória")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gramatura deve ser maior que zero")
    private BigDecimal grammage;

    /** Se nulo, assume {@code false} (obrigatório). */
    private Boolean isOptional;
}
