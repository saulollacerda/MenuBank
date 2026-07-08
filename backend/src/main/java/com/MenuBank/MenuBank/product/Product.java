package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.category.CatalogOrigin;
import com.MenuBank.MenuBank.category.Category;
import com.MenuBank.MenuBank.merchant.Merchant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "products")
@BatchSize(size = 50)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Merchant merchant;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "canonical_name")
    private String canonicalName;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    // columnDefinition default is required for dev (ddl-auto=update on non-empty tables);
    // prod relies on the matching Flyway migration.
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 20,
            columnDefinition = "varchar(20) not null default 'MENUBANK'")
    private CatalogOrigin origin = CatalogOrigin.MENUBANK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Include> includes = List.of();
}
