package com.lecook.backend.repository;

import com.lecook.backend.entity.RecipeIngredient;
import com.lecook.backend.entity.RecipeIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, RecipeIngredientId> {
}
