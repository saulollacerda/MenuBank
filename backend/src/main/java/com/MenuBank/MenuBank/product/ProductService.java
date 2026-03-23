package com.MenuBank.MenuBank.product;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsByName(request.getName())) {
            throw new DuplicateProductException("nome");
        }

        BigDecimal price = request.getPrice();
        BigDecimal estimatedCost = BigDecimal.ZERO;
        BigDecimal margin = price.subtract(estimatedCost);

        Product product = Product.builder()
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setMargin(request.getPrice().subtract(
                product.getEstimatedCost() != null ? product.getEstimatedCost() : BigDecimal.ZERO));

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
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
