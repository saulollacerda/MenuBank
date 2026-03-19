package com.MenuBank.MenuBank.user;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String restaurantName;
    private String cnpj;
    private String email;
    private String phone;
    private UserStatus status;
    private LocalDateTime createdAt;
}

