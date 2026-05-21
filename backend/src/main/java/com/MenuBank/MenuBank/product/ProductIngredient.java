package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.ingredient.Ingredient;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Associa um {@link Product} a um {@link Ingredient} com uma {@code grammage} específica.
 *
 * <p>Cada produto pode ter múltiplos ingredientes (sua "ficha técnica"). A gramatura é específica
 * deste produto — o mesmo ingrediente "Açaí Base" pode aparecer em "Açaí 330ml" com 200g e em
 * "Açaí 500ml" com 400g.</p>
 *
 * <p>O campo {@code isOptional} distingue ingredientes obrigatórios (parte da base do produto,
 * sempre consumidos: copo, colher, base) de ingredientes opcionais (complementos escolhidos pelo
 * cliente: granola, cobertura). Ingredientes opcionais só entram no custo do pedido quando
 * efetivamente aparecem nos {@code subItems} do pedido.</p>
 */
@Entity
@Table(name = "product_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    /**
     * Gramatura específica deste ingrediente neste produto (sobrepõe o
     * {@code defaultQuantity} do {@link Ingredient}).
     */
    @Column(nullable = false)
    private BigDecimal grammage;

    /**
     * Se {@code true}, o ingrediente só entra no custo quando aparece nos {@code subItems} do
     * pedido (complemento escolhido pelo cliente). Se {@code false}, sempre entra no custo
     * (parte da base do produto).
     */
    @Column(name = "is_optional", nullable = false)
    private boolean isOptional;
}
