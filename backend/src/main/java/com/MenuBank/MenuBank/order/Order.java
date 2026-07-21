package com.MenuBank.MenuBank.order;

import com.MenuBank.MenuBank.customer.Customer;
import com.MenuBank.MenuBank.fee.Fee;
import com.MenuBank.MenuBank.merchant.Merchant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Merchant merchant;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    /**
     * Última gravação do pedido, escrita pelo Hibernate na criação e em toda edição.
     * Serve para distinguir um pedido importado intacto de um que passou pela tela de
     * edição — informação que faltou ao diagnosticar o total errado em pedidos com frete.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = true)
    private Fee fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalValue;

    @Column(nullable = false)
    private BigDecimal estimatedProfit;

    @Column(name = "delivery_fee", precision = 19, scale = 4)
    private BigDecimal deliveryFee;

    /**
     * Taxa de serviço repassada ao iFood (pedidos do canal iFood importados via Anota.AI,
     * campo {@code additionalFees} do payload). Está inclusa no {@code totalValue}, mas não
     * é receita do restaurante — é deduzida do lucro e excluída da base da margem, como a
     * {@link #deliveryFee}. Nula em pedidos manuais e em pedidos sem taxa de serviço.
     */
    @Column(name = "service_fee", precision = 19, scale = 4)
    private BigDecimal serviceFee;

    @Column(name = "total_cost", precision = 19, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(name = "extra_info", length = 1024)
    private String extraInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin")
    private OrderOrigin origin;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items;

    /**
     * Snapshot da ficha do pedido: insumos cobrados UMA VEZ neste pedido, independentemente
     * da quantidade de itens (sacola, guardanapo). Copiado de {@link OrderFichaLine} na
     * criação/importação. Vazio/nulo em pedidos anteriores à V17 e em lojistas sem ficha
     * do pedido configurada — nesse caso o custo do pedido não muda.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 30)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderFichaIngredient> orderFicha;
}

