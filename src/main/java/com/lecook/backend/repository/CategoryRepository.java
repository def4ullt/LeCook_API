package com.lecook.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lecook.backend.entity.Category;
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
