package com.MenuBank.MenuBank.merchant;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String merchantName;

    @Column(unique = true, nullable = false, length = 14)
    private String cnpj;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "anota_ai_api_key", columnDefinition = "TEXT")
    private String anotaAiApiKey;
}
