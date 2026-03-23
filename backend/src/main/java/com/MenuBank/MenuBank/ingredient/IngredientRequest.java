package com.MenuBank.MenuBank.ingredient;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "Unidade é obrigatória")
    private String unit;

    @NotNull(message = "Custo por unidade é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Custo por unidade deve ser maior que zero")
    private BigDecimal costPerUnit;
}

