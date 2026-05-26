package com.MenuBank.MenuBank.merchant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> create(@Valid @RequestBody MerchantRequest request) {
        MerchantResponse response = merchantService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponse> findById(@PathVariable UUID id) {
        MerchantResponse response = merchantService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponse> update(@PathVariable UUID id, @Valid @RequestBody MerchantRequest request) {
        MerchantResponse response = merchantService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        merchantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/anota-ai-key")
    public ResponseEntity<MerchantResponse> updateAnotaAIKey(@RequestBody AnotaAIKeyRequest request) {
        MerchantResponse response = merchantService.updateAnotaAIKey(request);
        return ResponseEntity.ok(response);
    }
}
