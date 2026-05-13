package com.MenuBank.MenuBank.user;

import com.MenuBank.MenuBank.validation.PasswordMatch;
import com.MenuBank.MenuBank.validation.ValidCnpj;
import jakarta.validation.constraints.*;
import lombok.*;

@PasswordMatch
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank(message = "Nome do restaurante é obrigatório")
    private String restaurantName;

    @NotBlank(message = "CNPJ é obrigatório")
    @ValidCnpj
    private String cnpj;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    @Size(min = 6, message = "Confirmação de senha deve ter no mínimo 6 caracteres")
    private String confirmPassword;

    private String phone;
}
