package com.MenuBank.MenuBank.product;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products/{productId}/includes")
public class IncludeController {

    private final IncludeService includeService;

    public IncludeController(IncludeService includeService) {
        this.includeService = includeService;
    }

    @PostMapping
    public ResponseEntity<IncludeResponse> add(
            @PathVariable UUID productId,
            @Valid @RequestBody IncludeRequest request) {
        IncludeResponse response = includeService.add(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<IncludeResponse>> addBatch(
            @PathVariable UUID productId,
            @RequestBody List<@Valid IncludeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<IncludeResponse> response = includeService.addBatch(productId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IncludeResponse>> findByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(includeService.findByProductId(productId));
    }

    @PutMapping("/{includeId}")
    public ResponseEntity<IncludeResponse> update(
            @PathVariable UUID productId,
            @PathVariable UUID includeId,
            @Valid @RequestBody IncludeRequest request) {
        return ResponseEntity.ok(includeService.update(productId, includeId, request));
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<IncludeResponse>> reorder(
            @PathVariable UUID productId,
            @RequestBody List<@Valid IncludeReorderRequest> items) {
        return ResponseEntity.ok(includeService.reorder(productId, items));
    }

    @Transactional
    @DeleteMapping
    public ResponseEntity<Map<String, Long>> deleteAll(@PathVariable UUID productId) {
        long deleted = includeService.deleteAllByProductId(productId);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @Transactional
    @DeleteMapping("/{includeId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID productId,
            @PathVariable UUID includeId) {
        includeService.delete(productId, includeId);
        return ResponseEntity.noContent().build();
    }
}
