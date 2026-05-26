package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.merchant.Merchant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Merchant merchant;

    @Column(nullable = false)
    private String name;

    /**
     * Normalized name (lowercase, accent-free, whitespace-collapsed) used to match
     * orders against ingredients by name. Populated by {@code IngredientService} on
     * create/update via {@link IngredientNameNormalizer}.
     */
    @Column(name = "canonical_name")
    private String canonicalName;

    @Column(nullable = false)
    private String unit;

    /** Custo unitário (R$/unidade) cadastrado manualmente pelo restaurante. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal costPerUnit;

    /**
     * Preço de venda do complemento no cardápio Anota.AI (informativo).
     * NÃO substitui {@link #costPerUnit}.
     */
    @Column(name = "sale_price", precision = 19, scale = 4)
    private BigDecimal salePrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal defaultQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngredientStatus status;
}
