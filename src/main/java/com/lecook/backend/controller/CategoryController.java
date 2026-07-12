package com.lecook.backend.controller;


import com.lecook.backend.entity.Category;
import com.lecook.backend.entity.Ingredient;
import com.lecook.backend.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }


    @GetMapping
    public List<Category> getAll() {
        return service.getAll();
    }


    @PostMapping
    public Category create(@RequestBody Category category) {
        return service.create(category);
    }

    @GetMapping("/{id}")
    public Category get(@PathVariable Integer id) {
        return service.getById(id);
    }


    @PutMapping("/{id}")
    public Category update(@PathVariable Integer id, @RequestBody Category category) {
        return service.update(id, category);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}