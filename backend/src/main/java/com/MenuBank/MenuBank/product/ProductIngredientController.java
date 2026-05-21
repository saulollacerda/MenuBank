package com.MenuBank.MenuBank.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/ingredients")
public class ProductIngredientController {

    private final ProductIngredientService productIngredientService;

    public ProductIngredientController(ProductIngredientService productIngredientService) {
        this.productIngredientService = productIngredientService;
    }

    @PostMapping
    public ResponseEntity<ProductIngredientResponse> addProductIngredient(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductIngredientRequest request) {
        ProductIngredientResponse response = productIngredientService.addProductIngredient(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductIngredientResponse>> addProductIngredientsBatch(
            @PathVariable UUID productId,
            @RequestBody List<@Valid ProductIngredientRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductIngredientResponse> response = productIngredientService.addProductIngredientsBatch(productId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProductIngredientResponse>> findByProductId(@PathVariable UUID productId) {
        List<ProductIngredientResponse> responses = productIngredientService.findByProductId(productId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{productIngredientId}")
    public ResponseEntity<ProductIngredientResponse> update(
            @PathVariable UUID productId,
            @PathVariable UUID productIngredientId,
            @Valid @RequestBody ProductIngredientRequest request) {
        ProductIngredientResponse response = productIngredientService.update(productId, productIngredientId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualiza apenas a gramatura, referenciando o ingrediente pelo seu ID (não pelo ID da junção).
     */
    @PutMapping("/{ingredientId}/grammage")
    public ResponseEntity<ProductIngredientResponse> updateGrammage(
            @PathVariable UUID productId,
            @PathVariable UUID ingredientId,
            @Valid @RequestBody UpdateGrammageRequest request) {
        ProductIngredientResponse response = productIngredientService
                .updateGrammageByIngredientId(productId, ingredientId, request.getGrammage());
        return ResponseEntity.ok(response);
    }

    @Transactional
    @DeleteMapping
    public ResponseEntity<Map<String, Long>> deleteAll(@PathVariable UUID productId) {
        long deleted = productIngredientService.deleteAllByProductId(productId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @Transactional
    @DeleteMapping("/{productIngredientId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID productId,
            @PathVariable UUID productIngredientId) {
        productIngredientService.delete(productId, productIngredientId);
        return ResponseEntity.noContent().build();
    }
}
