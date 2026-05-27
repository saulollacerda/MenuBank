package com.MenuBank.MenuBank.category;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        CategoryResponse response = categoryService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponse>> findAll(
            @RequestParam(required = false, defaultValue = "") String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(categoryService.findAll(search, pageable));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<CategoryRevenueResponse>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(categoryService.revenue(startDate, endDate));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
