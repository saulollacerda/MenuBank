package com.MenuBank.MenuBank.ingredient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingredient-categories")
public class IngredientCategoryController {

    private final IngredientCategoryService ingredientCategoryService;

    public IngredientCategoryController(IngredientCategoryService ingredientCategoryService) {
        this.ingredientCategoryService = ingredientCategoryService;
    }

    @PostMapping
    public ResponseEntity<IngredientCategoryResponse> create(@Valid @RequestBody IngredientCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredientCategoryService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<IngredientCategoryResponse>> findAll() {
        return ResponseEntity.ok(ingredientCategoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientCategoryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ingredientCategoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientCategoryResponse> update(@PathVariable UUID id,
                                                              @Valid @RequestBody IngredientCategoryRequest request) {
        return ResponseEntity.ok(ingredientCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingredientCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
