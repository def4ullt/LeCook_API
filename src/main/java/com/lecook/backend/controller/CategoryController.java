package com.lecook.backend.controller;

import com.lecook.backend.dto.NamedRequest;
import com.lecook.backend.dto.NamedResponse;
import com.lecook.backend.entity.Category;
import com.lecook.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/api/category", "/api/categories"})
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }
    private NamedResponse response(Category value) { return new NamedResponse(value.getId(), value.getName()); }
    private Category entity(NamedRequest request) { var value = new Category(); value.setName(request.name().trim()); return value; }
    @GetMapping public List<NamedResponse> all() { return service.getAll().stream().map(this::response).toList(); }
    @GetMapping("/{id}") public NamedResponse get(@PathVariable Integer id) { return response(service.getById(id)); }
    @PostMapping public ResponseEntity<NamedResponse> create(@Valid @RequestBody NamedRequest request) {
        var result = response(service.create(entity(request)));
        return ResponseEntity.created(URI.create("/api/categories/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public NamedResponse update(@PathVariable Integer id, @Valid @RequestBody NamedRequest request) { return response(service.update(id, entity(request))); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.getById(id); service.delete(id); return ResponseEntity.noContent().build();
    }
}
