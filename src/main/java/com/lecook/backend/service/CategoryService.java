package com.lecook.backend.service;

import com.lecook.backend.entity.Category;
import com.lecook.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }


    public List<Category> getAll() {
        return repository.findAll();
    }


    public Category create(Category category) {
        return repository.save(category);
    }

    public Category getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }

    public Category update(Integer id, Category category) {

        Category existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        existing.setName(category.getName());

        return repository.save(existing);
    }
}
