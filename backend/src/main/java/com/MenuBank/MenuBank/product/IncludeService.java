package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.common.MerchantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class IncludeService {

    private final IncludeRepository includeRepository;
    private final ProductRepository productRepository;
    private final MerchantContext merchantContext;

    public IncludeService(IncludeRepository includeRepository,
                          ProductRepository productRepository,
                          MerchantContext merchantContext) {
        this.includeRepository = includeRepository;
        this.productRepository = productRepository;
        this.merchantContext = merchantContext;
    }

    @Transactional
    public IncludeResponse add(UUID productId, IncludeRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        Product product = productRepository.findByIdAndMerchantId(productId, merchantId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Include saved = includeRepository.save(buildInclude(product, request));
        return toResponse(saved);
    }

    @Transactional
    public List<IncludeResponse> addBatch(UUID productId, List<IncludeRequest> requests) {
        UUID merchantId = merchantContext.getMerchantId();

        Product product = productRepository.findByIdAndMerchantId(productId, merchantId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        List<Include> toSave = requests.stream()
                .map(req -> buildInclude(product, req))
                .toList();

        return toSave.stream()
                .map(includeRepository::save)
                .map(this::toResponse)
                .toList();
    }

    public List<IncludeResponse> findByProductId(UUID productId) {
        UUID merchantId = merchantContext.getMerchantId();

        if (!productRepository.existsByIdAndMerchantId(productId, merchantId)) {
            throw new ProductNotFoundException(productId);
        }
        return includeRepository.findByProductIdAndProductMerchantId(productId, merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public IncludeResponse update(UUID productId, UUID includeId, IncludeRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        Include include = includeRepository
                .findByIdAndProductIdAndProductMerchantId(includeId, productId, merchantId)
                .orElseThrow(() -> new IncludeNotFoundException(includeId));

        include.setName(request.getName());
        include.setCost(request.getCost());
        include.setQuantity(request.getQuantity() != null ? request.getQuantity() : BigDecimal.ONE);

        return toResponse(includeRepository.save(include));
    }

    @Transactional
    public long deleteAllByProductId(UUID productId) {
        UUID merchantId = merchantContext.getMerchantId();

        if (!productRepository.existsByIdAndMerchantId(productId, merchantId)) {
            throw new ProductNotFoundException(productId);
        }
        return includeRepository.deleteAllByProductIdAndProductMerchantId(productId, merchantId);
    }

    @Transactional
    public void delete(UUID productId, UUID includeId) {
        UUID merchantId = merchantContext.getMerchantId();

        if (includeRepository.findByIdAndProductIdAndProductMerchantId(includeId, productId, merchantId).isEmpty()) {
            throw new IncludeNotFoundException(includeId);
        }
        includeRepository.deleteByIdAndProductIdAndProductMerchantId(includeId, productId, merchantId);
    }

    private Include buildInclude(Product product, IncludeRequest request) {
        BigDecimal quantity = request.getQuantity() != null ? request.getQuantity() : BigDecimal.ONE;
        return Include.builder()
                .product(product)
                .name(request.getName())
                .cost(request.getCost())
                .quantity(quantity)
                .build();
    }

    private IncludeResponse toResponse(Include include) {
        BigDecimal totalCost = include.getCost().multiply(include.getQuantity());
        return IncludeResponse.builder()
                .id(include.getId())
                .productId(include.getProduct().getId())
                .name(include.getName())
                .cost(include.getCost())
                .quantity(include.getQuantity())
                .totalCost(totalCost)
                .build();
    }
}
