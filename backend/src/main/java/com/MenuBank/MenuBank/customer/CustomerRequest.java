package com.MenuBank.MenuBank.customer;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    private String phone;

    @Email(message = "Email inválido")
    private String email;
}

