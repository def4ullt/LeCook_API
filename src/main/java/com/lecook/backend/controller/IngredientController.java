package com.lecook.backend.controller;

import com.lecook.backend.dto.NamedRequest;
import com.lecook.backend.dto.NamedResponse;
import com.lecook.backend.entity.Ingredient;
import com.lecook.backend.service.IngredientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/api/ingredient", "/api/ingredients"})
public class IngredientController {
    private final IngredientService service;
    public IngredientController(IngredientService service) { this.service = service; }
    private NamedResponse response(Ingredient value) { return new NamedResponse(value.getId(), value.getName()); }
    private Ingredient entity(NamedRequest request) { var value = new Ingredient(); value.setName(request.name().trim()); return value; }
    @GetMapping public List<NamedResponse> all() { return service.getAll().stream().map(this::response).toList(); }
    @GetMapping("/{id}") public NamedResponse get(@PathVariable Integer id) { return response(service.getById(id)); }
    @PostMapping public ResponseEntity<NamedResponse> create(@Valid @RequestBody NamedRequest request) {
        var result = response(service.create(entity(request)));
        return ResponseEntity.created(URI.create("/api/ingredients/" + result.id())).body(result);
    }
    @PutMapping("/{id}") public NamedResponse update(@PathVariable Integer id, @Valid @RequestBody NamedRequest request) { return response(service.update(id, entity(request))); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.getById(id); service.delete(id); return ResponseEntity.noContent().build();
    }
}
