package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.common.MerchantContext;
import com.MenuBank.MenuBank.merchant.MerchantRepository;
import com.MenuBank.MenuBank.notification.NotificationService;
import com.MenuBank.MenuBank.product.IncludeRepository;
import com.MenuBank.MenuBank.product.IngredientProductUsageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final MerchantRepository merchantRepository;
    private final NotificationService notificationService;
    private final MerchantContext merchantContext;
    private final IncludeRepository includeRepository;

    public IngredientService(IngredientRepository ingredientRepository,
                             MerchantRepository merchantRepository,
                             NotificationService notificationService,
                             MerchantContext merchantContext,
                             IncludeRepository includeRepository) {
        this.ingredientRepository = ingredientRepository;
        this.merchantRepository = merchantRepository;
        this.notificationService = notificationService;
        this.merchantContext = merchantContext;
        this.includeRepository = includeRepository;
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        UUID merchantId = merchantContext.getMerchantId();

        if (ingredientRepository.existsByNameAndMerchantId(request.getName(), merchantId)) {
            throw new DuplicateIngredientException("nome");
        }

        String canonicalName = IngredientNameNormalizer.normalize(request.getName());
        Ingredient ingredient = Ingredient.builder()
                .merchant(merchantRepository.getReferenceById(merchantId))
                .name(request.getName())
                .canonicalName(canonicalName)
                .unit(request.getUnit())
                .costPerUnit(request.getCostPerUnit())
                .defaultQuantity(request.getDefaultQuantity())
                .status(IngredientStatus.ACTIVE)
                .build();

        Ingredient saved = ingredientRepository.save(ingredient);
        notificationService.resolveMissingIngredient(canonicalName, merchantId);
        return toResponse(saved);
    }

    public IngredientResponse findById(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Ingredient ingredient = ingredientRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return toResponse(ingredient);
    }

    public Page<IngredientResponse> findAll(String search, Pageable pageable) {
        UUID merchantId = merchantContext.getMerchantId();
        String term = search == null ? "" : search;
        return ingredientRepository.findAllByMerchantIdAndNameContainingIgnoreCase(merchantId, term, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public IngredientResponse update(UUID id, IngredientRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Ingredient ingredient = ingredientRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        ingredient.setName(request.getName());
        ingredient.setCanonicalName(IngredientNameNormalizer.normalize(request.getName()));
        ingredient.setUnit(request.getUnit());
        ingredient.setCostPerUnit(request.getCostPerUnit());
        ingredient.setDefaultQuantity(request.getDefaultQuantity());

        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientResponse updateCost(UUID id, IngredientCostRequest request) {
        UUID merchantId = merchantContext.getMerchantId();
        Ingredient ingredient = ingredientRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        ingredient.setCostPerUnit(request.getCostPerUnit());
        if (request.getDefaultQuantity() != null) {
            ingredient.setDefaultQuantity(request.getDefaultQuantity());
        }
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            ingredient.setUnit(request.getUnit());
        }

        return toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public void delete(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        if (!ingredientRepository.existsByIdAndMerchantId(id, merchantId)) {
            throw new IngredientNotFoundException(id);
        }
        ingredientRepository.deleteByIdAndMerchantId(id, merchantId);
    }

    /**
     * Retorna os produtos cujas fichas tecnicas (includes) contem este ingrediente
     * (match por nome, case-insensitive).
     */
    public List<IngredientProductUsageResponse> fetchUsages(UUID id) {
        UUID merchantId = merchantContext.getMerchantId();
        Ingredient ingredient = ingredientRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new IngredientNotFoundException(id));

        return includeRepository
                .findByNameIgnoreCaseAndProductMerchantId(ingredient.getName(), merchantId)
                .stream()
                .map(inc -> {
                    BigDecimal cost = inc.getCost() != null ? inc.getCost() : BigDecimal.ZERO;
                    BigDecimal qty = inc.getQuantity() != null ? inc.getQuantity() : BigDecimal.ONE;
                    BigDecimal total = cost.multiply(qty).setScale(4, RoundingMode.HALF_UP);
                    return IngredientProductUsageResponse.builder()
                            .includeId(inc.getId())
                            .productId(inc.getProduct().getId())
                            .productName(inc.getProduct().getName())
                            .quantity(qty)
                            .cost(cost)
                            .totalCost(total)
                            .build();
                })
                .toList();
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .unit(ingredient.getUnit())
                .costPerUnit(ingredient.getCostPerUnit())
                .salePrice(ingredient.getSalePrice())
                .defaultQuantity(ingredient.getDefaultQuantity())
                .status(ingredient.getStatus())
                .build();
    }
}
