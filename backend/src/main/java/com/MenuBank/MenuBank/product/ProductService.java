package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.common.UserContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserContext userContext;

    public ProductService(ProductRepository productRepository, UserContext userContext) {
        this.productRepository = productRepository;
        this.userContext = userContext;
    }

    public ProductResponse create(ProductRequest request) {
        UUID ownerId = userContext.getUserId();

        if (productRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateProductException("nome");
        }

        BigDecimal price = request.getPrice();
        BigDecimal estimatedCost = BigDecimal.ZERO;
        BigDecimal margin = price.subtract(estimatedCost);

        Product product = Product.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .price(price)
                .estimatedCost(estimatedCost)
                .margin(margin)
                .status(ProductStatus.ACTIVE)
                .cmv(BigDecimal.ZERO)
                .build();

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public ProductResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Product product = productRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    public List<ProductResponse> findAll() {
        UUID ownerId = userContext.getUserId();
        return productRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        UUID ownerId = userContext.getUserId();
        Product product = productRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setMargin(request.getPrice().subtract(
                product.getEstimatedCost() != null ? product.getEstimatedCost() : BigDecimal.ZERO));

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        UUID ownerId = userContext.getUserId();
        if (!productRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteByIdAndOwnerId(id, ownerId);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .estimatedCost(product.getEstimatedCost())
                .margin(product.getMargin())
                .status(product.getStatus())
                .cmv(product.getCmv())
                .build();
    }
}
