package com.MenuBank.MenuBank.ingredient;

import com.MenuBank.MenuBank.product.IngredientProductUsageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientRequest request) {
        IngredientResponse response = ingredientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponse> findById(@PathVariable UUID id) {
        IngredientResponse response = ingredientService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<IngredientResponse>> findAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ingredientService.findAll(search, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientResponse> update(@PathVariable UUID id, @Valid @RequestBody IngredientRequest request) {
        IngredientResponse response = ingredientService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Atualização atômica dos campos relacionados ao custo manual do ingrediente.
     * Não toca em {@code salePrice} (sincronizado do Anota.AI).
     */
    @PutMapping("/{id}/cost")
    public ResponseEntity<IngredientResponse> updateCost(@PathVariable UUID id, @Valid @RequestBody IngredientCostRequest request) {
        IngredientResponse response = ingredientService.updateCost(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/usages")
    public ResponseEntity<List<IngredientProductUsageResponse>> fetchUsages(@PathVariable UUID id) {
        return ResponseEntity.ok(ingredientService.fetchUsages(id));
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
