package com.MenuBank.MenuBank.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/recipe-items")
public class RecipeItemController {

    private final RecipeItemService recipeItemService;

    public RecipeItemController(RecipeItemService recipeItemService) {
        this.recipeItemService = recipeItemService;
    }

    @PostMapping
    public ResponseEntity<RecipeItemResponse> addRecipeItem(
            @PathVariable UUID productId,
            @Valid @RequestBody RecipeItemRequest request) {
        RecipeItemResponse response = recipeItemService.addRecipeItem(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecipeItemResponse>> findByProductId(@PathVariable UUID productId) {
        List<RecipeItemResponse> responses = recipeItemService.findByProductId(productId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{recipeItemId}")
    public ResponseEntity<RecipeItemResponse> update(
            @PathVariable UUID productId,
            @PathVariable UUID recipeItemId,
            @Valid @RequestBody RecipeItemRequest request) {
        RecipeItemResponse response = recipeItemService.update(productId, recipeItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recipeItemId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID productId,
            @PathVariable UUID recipeItemId) {
        recipeItemService.delete(productId, recipeItemId);
        return ResponseEntity.noContent().build();
    }
}
