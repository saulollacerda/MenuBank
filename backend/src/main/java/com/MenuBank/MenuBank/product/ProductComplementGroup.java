package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.ingredient.IngredientCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_complement_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductComplementGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_category_id", nullable = false)
    private IngredientCategory ingredientCategory;

    @Column(nullable = false)
    private int minRequired;

    @Column(nullable = false)
    private int maxAllowed;
}
