package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.common.UserContext;
import com.MenuBank.MenuBank.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final NotificationService notificationService;
    private final UserContext userContext;

    public IngredientService(IngredientRepository ingredientRepository,
                             NotificationService notificationService,
                             UserContext userContext) {
        this.ingredientRepository = ingredientRepository;
        this.notificationService = notificationService;
        this.userContext = userContext;
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        UUID ownerId = userContext.getUserId();

        if (ingredientRepository.existsByNameAndOwnerId(request.getName(), ownerId)) {
            throw new DuplicateIngredientException("nome");
        }

        String canonicalName = IngredientNameNormalizer.normalize(request.getName());
        Ingredient ingredient = Ingredient.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .canonicalName(canonicalName)
                .unit(request.getUnit())
                .costPerUnit(request.getCostPerUnit())
                .defaultQuantity(request.getDefaultQuantity())
                .status(IngredientStatus.ACTIVE)
                .build();

        Ingredient saved = ingredientRepository.save(ingredient);
        notificationService.resolveMissingIngredient(canonicalName, ownerId);
        return toResponse(saved);
    }

    public IngredientResponse findById(UUID id) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return toResponse(ingredient);
    }

    public Page<IngredientResponse> findAll(String search, Pageable pageable) {
        UUID ownerId = userContext.getUserId();
        String term = search == null ? "" : search;
        return ingredientRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, term, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public IngredientResponse update(UUID id, IngredientRequest request) {
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
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
        UUID ownerId = userContext.getUserId();
        Ingredient ingredient = ingredientRepository.findByIdAndOwnerId(id, ownerId)
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
        UUID ownerId = userContext.getUserId();
        if (!ingredientRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new IngredientNotFoundException(id);
        }
        ingredientRepository.deleteByIdAndOwnerId(id, ownerId);
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
