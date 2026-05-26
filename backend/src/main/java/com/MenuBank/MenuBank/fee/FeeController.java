package com.MenuBank.MenuBank.fee;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    @PostMapping
    public ResponseEntity<FeeResponse> create(@Valid @RequestBody FeeRequest request) {
        FeeResponse response = feeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeResponse> findById(@PathVariable UUID id) {
        FeeResponse response = feeService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<FeeResponse>> findAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(feeService.findAll(search, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeeResponse> update(@PathVariable UUID id,
                                              @Valid @RequestBody FeeRequest request) {
        FeeResponse response = feeService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        feeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
