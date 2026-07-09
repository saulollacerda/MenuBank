package com.MenuBank.MenuBank.merchant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
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

    @Column(nullable = true)
    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "anota_ai_api_key", columnDefinition = "TEXT")
    private String anotaAiApiKey;

    @Column(name = "ifood_merchant_id")
    private String ifoodMerchantId;

    @Column(name = "ifood_authorized_at")
    private LocalDateTime ifoodAuthorizedAt;

    // columnDefinition default is required for dev (ddl-auto=update on non-empty tables);
    // prod relies on the matching Flyway migration.
    @Builder.Default
    @Column(name = "ifood_order_sync_enabled", nullable = false,
            columnDefinition = "boolean not null default false")
    private boolean ifoodOrderSyncEnabled = false;

    @Column(name = "ifood_catalog_imported_at")
    private LocalDateTime ifoodCatalogImportedAt;

    @Column(length = 500)
    private String address;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private List<OpeningHour> openingHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb")
    private MerchantPreferences preferences;
}
