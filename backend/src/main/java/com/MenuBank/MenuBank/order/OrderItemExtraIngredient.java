package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_extra_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemExtraIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    /**
     * Extra ingredient quantity per ordered product unit.
     */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /**
     * Snapshot of the ingredient cost at the time the order was created/updated.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal costPerUnit;

    @Column(nullable = false)
    private String ingredientName;

    @Column(nullable = false)
    private String ingredientUnit;
}

