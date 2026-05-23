package com.MenuBank.MenuBank.product;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.ingredient.Ingredient;
import com.MenuBank.MenuBank.ingredient.IngredientNotFoundException;
import com.MenuBank.MenuBank.ingredient.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductIngredientService {

    private final ProductIngredientRepository productIngredientRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final UserContext userContext;

    public ProductIngredientService(ProductIngredientRepository productIngredientRepository,
                                    ProductRepository productRepository,
                                    IngredientRepository ingredientRepository,
                                    UserContext userContext) {
        this.productIngredientRepository = productIngredientRepository;
        this.productRepository = productRepository;
        this.ingredientRepository = ingredientRepository;
        this.userContext = userContext;
    }

    @Transactional
    public ProductIngredientResponse addProductIngredient(UUID productId, ProductIngredientRequest request) {
        UUID ownerId = userContext.getUserId();

        Product product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductIngredient saved = productIngredientRepository.save(buildProductIngredient(product, request, ownerId));
        return toResponse(saved);
    }

    @Transactional
    public List<ProductIngredientResponse> addProductIngredientsBatch(UUID productId, List<ProductIngredientRequest> requests) {
        UUID ownerId = userContext.getUserId();

        Product product = productRepository.findByIdAndOwnerId(productId, ownerId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // Fail-fast: valida e constrói todos os ProductIngredients antes de salvar qualquer um
        List<ProductIngredient> toSave = requests.stream()
                .map(req -> buildProductIngredient(product, req, ownerId))
                .toList();

        return toSave.stream()
                .map(productIngredientRepository::save)
                .map(this::toResponse)
                .toList();
    }

    private ProductIngredient buildProductIngredient(Product product, ProductIngredientRequest request, UUID ownerId) {
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(request.getIngredientId(), ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(request.getIngredientId()));
        return ProductIngredient.builder()
                .product(product)
                .ingredient(ingredient)
                .grammage(request.getGrammage())
                .isOptional(Boolean.TRUE.equals(request.getIsOptional()))
                .build();
    }

    public List<ProductIngredientResponse> findByProductId(UUID productId) {
        UUID ownerId = userContext.getUserId();

        if (!productRepository.existsByIdAndOwnerId(productId, ownerId)) {
            throw new ProductNotFoundException(productId);
        }
        return productIngredientRepository.findByProductIdAndProductOwnerId(productId, ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductIngredientResponse update(UUID productId, UUID productIngredientId, ProductIngredientRequest request) {
        UUID ownerId = userContext.getUserId();

        ProductIngredient productIngredient = productIngredientRepository
                .findByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId)
                .orElseThrow(() -> new ProductIngredientNotFoundException(productIngredientId));

        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(request.getIngredientId(), ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(request.getIngredientId()));

        productIngredient.setIngredient(ingredient);
        productIngredient.setGrammage(request.getGrammage());
        if (request.getIsOptional() != null) {
            productIngredient.setOptional(request.getIsOptional());
        }

        ProductIngredient saved = productIngredientRepository.save(productIngredient);

        return toResponse(saved);
    }

    /**
     * Atualiza apenas a gramatura de um ingrediente associado ao produto, identificando-o pelo
     * {@code ingredientId} (não pelo ID da junção). Útil para integrações que conhecem o ingrediente
     * mas não o ID da {@link ProductIngredient}.
     */
    @Transactional
    public ProductIngredientResponse updateGrammageByIngredientId(UUID productId, UUID ingredientId, BigDecimal grammage) {
        UUID ownerId = userContext.getUserId();

        if (!productRepository.existsByIdAndOwnerId(productId, ownerId)) {
            throw new ProductNotFoundException(productId);
        }

        ProductIngredient productIngredient = productIngredientRepository
                .findByProductIdAndIngredientIdAndProductOwnerId(productId, ingredientId, ownerId)
                .orElseThrow(() -> new ProductIngredientNotFoundException(ingredientId));

        productIngredient.setGrammage(grammage);
        return toResponse(productIngredientRepository.save(productIngredient));
    }

    @Transactional
    public long deleteAllByProductId(UUID productId) {
        UUID ownerId = userContext.getUserId();

        if (!productRepository.existsByIdAndOwnerId(productId, ownerId)) {
            throw new ProductNotFoundException(productId);
        }
        return productIngredientRepository.deleteAllByProductIdAndProductOwnerId(productId, ownerId);
    }

    @Transactional
    public void delete(UUID productId, UUID productIngredientId) {
        UUID ownerId = userContext.getUserId();

        if (productIngredientRepository.findByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId).isEmpty()) {
            throw new ProductIngredientNotFoundException(productIngredientId);
        }
        productIngredientRepository.deleteByIdAndProductIdAndProductOwnerId(productIngredientId, productId, ownerId);
    }

    private ProductIngredientResponse toResponse(ProductIngredient productIngredient) {
        Ingredient ingredient = productIngredient.getIngredient();
        BigDecimal costPerUnit = ingredient.getCostPerUnit() != null ? ingredient.getCostPerUnit() : BigDecimal.ZERO;
        BigDecimal totalCost = productIngredient.getGrammage().multiply(costPerUnit);

        return ProductIngredientResponse.builder()
                .id(productIngredient.getId())
                .productId(productIngredient.getProduct().getId())
                .ingredientId(ingredient.getId())
                .ingredientName(ingredient.getName())
                .ingredientUnit(ingredient.getUnit())
                .grammage(productIngredient.getGrammage())
                .isOptional(productIngredient.isOptional())
                .costPerUnit(costPerUnit)
                .totalCost(totalCost)
                .build();
    }
}
