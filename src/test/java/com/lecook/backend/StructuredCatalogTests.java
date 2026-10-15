package com.lecook.backend;

import com.lecook.backend.config.CatalogImport;
import com.lecook.backend.dto.SyncRequest;
import com.lecook.backend.service.RecipeService;
import com.lecook.backend.service.RecipeSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StructuredCatalogTests extends IsolatedDatabase {
    @Autowired JdbcTemplate db;
    @Autowired PlatformTransactionManager manager;
    @Autowired RecipeService recipes;
    @Autowired RecipeSyncService sync;

    @Test void catalogImportIsIdempotentAndSyncCarriesRealComposition() throws Exception {
        var importer = new CatalogImport(db, manager);
        importer.run(new DefaultApplicationArguments());
        assertEquals(60, db.queryForObject("SELECT count(*) FROM recipes", Integer.class));
        int links = db.queryForObject("SELECT count(*) FROM recipe_ingredients", Integer.class);
        importer.run(new DefaultApplicationArguments());
        assertEquals(60, db.queryForObject("SELECT count(*) FROM recipes", Integer.class));
        assertEquals(links, db.queryForObject("SELECT count(*) FROM recipe_ingredients", Integer.class));
        assertEquals(180, db.queryForObject("SELECT count(*) FROM recipe_steps", Integer.class));
        assertTrue(db.queryForObject("SELECT EXISTS(SELECT 1 FROM recipe_ingredients GROUP BY ingredient_id HAVING count(DISTINCT recipe_id)>1)", Boolean.class));
        var page = sync.page(new SyncRequest(List.of()));
        assertEquals(60, page.recipes().size());
        assertFalse(page.hasMore());
        for (var recipe : page.recipes()) {
            assertNull(recipe.calories());
            assertTrue(recipe.details().servings() > 0);
            assertTrue(recipe.details().ingredients().size() >= 4);
            assertEquals(3, recipe.details().steps().size());
            assertTrue(recipe.details().ingredients().stream().allMatch(i -> i.quantity().signum() > 0 && !i.unit().isBlank()));
            assertEquals(recipe.details(), recipes.getById(recipe.id()).details());
        }
        assertTrue(sync.page(new SyncRequest(page.recipes().stream().map(r -> r.id()).toList())).recipes().isEmpty());
    }
}
