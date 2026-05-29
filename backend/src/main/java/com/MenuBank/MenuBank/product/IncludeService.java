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
        return includeRepository.findByProductIdAndProductMerchantIdOrderBySortOrderAsc(productId, merchantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<IncludeResponse> reorder(UUID productId, List<IncludeReorderRequest> items) {
        UUID merchantId = merchantContext.getMerchantId();

        if (!productRepository.existsByIdAndMerchantId(productId, merchantId)) {
            throw new ProductNotFoundException(productId);
        }
        for (IncludeReorderRequest item : items) {
            Include include = includeRepository
                    .findByIdAndProductIdAndProductMerchantId(item.getId(), productId, merchantId)
                    .orElseThrow(() -> new IncludeNotFoundException(item.getId()));
            include.setSortOrder(item.getSortOrder());
            includeRepository.save(include);
        }
        return includeRepository
                .findByProductIdAndProductMerchantIdOrderBySortOrderAsc(productId, merchantId)
                .stream()
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
        if (request.getKind() != null) {
            include.setKind(request.getKind());
        }
        if (request.getSortOrder() != null) {
            include.setSortOrder(request.getSortOrder());
        }

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
        UUID merchantId = product.getMerchant().getId();
        BigDecimal quantity = request.getQuantity() != null ? request.getQuantity() : BigDecimal.ONE;
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            Integer max = includeRepository.findMaxSortOrderByProductIdAndProductMerchantId(product.getId(), merchantId);
            sortOrder = (max == null ? 0 : max) + 1;
        }
        return Include.builder()
                .product(product)
                .name(request.getName())
                .cost(request.getCost())
                .quantity(quantity)
                .kind(request.getKind() != null ? request.getKind() : IncludeKind.INGREDIENT)
                .sortOrder(sortOrder)
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
                .kind(include.getKind() != null ? include.getKind() : IncludeKind.INGREDIENT)
                .sortOrder(include.getSortOrder())
                .build();
    }
}
