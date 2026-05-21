package com.MenuBank.MenuBank.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGrammageRequest {

    @NotNull(message = "Gramatura é obrigatória")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gramatura deve ser maior que zero")
    private BigDecimal grammage;
}
