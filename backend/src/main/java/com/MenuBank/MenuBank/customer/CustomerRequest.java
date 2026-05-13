package com.MenuBank.MenuBank.customer;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome não pode ter mais de 255 caracteres")
    private String name;

    @Size(max = 20, message = "Telefone não pode ter mais de 20 caracteres")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Telefone contém caracteres inválidos")
    private String phone;

    @Email(message = "Email inválido")
    @Size(max = 255, message = "Email não pode ter mais de 255 caracteres")
    private String email;
}
