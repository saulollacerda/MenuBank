package com.MenuBank.MenuBank.ingredient;

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

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit;

    /** Custo unitário (R$/unidade) cadastrado manualmente pelo restaurante. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal costPerUnit;

    /**
     * Preço de venda do complemento no cardápio Anota.AI (informativo).
     * Sincronizado a partir do {@code price} retornado pela API; NÃO substitui {@link #costPerUnit}.
     */
    @Column(name = "sale_price", precision = 19, scale = 4)
    private BigDecimal salePrice;

    @Column(precision = 19, scale = 4)
    private BigDecimal defaultQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngredientStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_category_id")
    private IngredientCategory category;

    @Column(name = "external_id")
    private String externalId;
}