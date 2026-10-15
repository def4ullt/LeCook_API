package com.lecook.backend.repository;

import com.lecook.backend.entity.RecipeIngredient;
import com.lecook.backend.entity.RecipeIngredientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, RecipeIngredientId> {
    @org.springframework.data.jpa.repository.Query("select ri from RecipeIngredient ri join fetch ri.ingredient where ri.recipe.ID=:id order by ri.ingredient.name")
    java.util.List<RecipeIngredient> forRecipe(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("delete from RecipeIngredient ri where ri.recipe.ID=:id")
    void deleteForRecipe(@org.springframework.data.repository.query.Param("id") java.util.UUID id);
}
