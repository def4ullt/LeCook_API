package com.lecook.backend.service;

import java.util.*;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecipeDetailsService {
    public record Ingredient(int ingredientId, String name, BigDecimal quantity, String unit) {}
    public record Details(Integer servings, Integer preparationMinutes, List<Ingredient> ingredients, List<String> steps) {}
    private final JdbcTemplate db;
    public RecipeDetailsService(JdbcTemplate db) { this.db = db; }
    public Map<UUID, Details> load(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var result = new HashMap<UUID, Details>();
        Object[] params = ids.toArray();
        db.query("SELECT id,servings,preparation_minutes FROM recipes WHERE id IN (" + placeholders + ")", rs -> {
            result.put(rs.getObject(1, UUID.class), new Details(rs.getObject(2, Integer.class), rs.getObject(3, Integer.class), new ArrayList<>(), new ArrayList<>()));
        }, params);
        db.query("SELECT ri.recipe_id,i.id,i.name,ri.quantity,ri.unit FROM recipe_ingredients ri JOIN ingredients i ON i.id=ri.ingredient_id WHERE ri.recipe_id IN (" + placeholders + ") ORDER BY i.name", rs -> {
            result.get(rs.getObject(1, UUID.class)).ingredients().add(new Ingredient(rs.getInt(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5)));
        }, params);
        db.query("SELECT recipe_id,instruction FROM recipe_steps WHERE recipe_id IN (" + placeholders + ") ORDER BY recipe_id,step_number", rs -> {
            result.get(rs.getObject(1, UUID.class)).steps().add(rs.getString(2));
        }, params);
        return result;
    }
}
