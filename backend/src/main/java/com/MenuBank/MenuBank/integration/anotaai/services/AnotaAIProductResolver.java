package com.MenuBank.MenuBank.integration.anotaai.services;

import com.MenuBank.MenuBank.integration.anotaai.AnotaAIOrderDetailResponse;
import com.MenuBank.MenuBank.product.Product;
import com.MenuBank.MenuBank.product.ProductRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolve um {@link Product} para um item de pedido do Anota.AI.
 *
 * <p>Apenas pedidos Anota.AI são importados — o campo {@code internalId} sempre
 * contém o ID do produto no MenuBank, portanto a resolução é direta por ID.
 */
public class AnotaAIProductResolver {

    private final ProductRepository productRepository;

    public AnotaAIProductResolver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Optional<Product> resolve(AnotaAIOrderDetailResponse.AnotaAIOrderItem remoteItem,
                                      UUID merchantId) {
        String internalId = remoteItem.getInternalId();
        if (internalId == null || internalId.isBlank()) return Optional.empty();
        return productRepository.findByExternalIdAndMerchantId(internalId, merchantId);
    }
}
