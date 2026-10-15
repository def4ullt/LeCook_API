package com.lecook.backend.config;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Explicit, atomic import. Never runs during ordinary application startup. */
@Component
@ConditionalOnProperty(name="lecook.catalog.import", havingValue="true")
public class CatalogImport implements ApplicationRunner {
    private final JdbcTemplate db;
    private final TransactionTemplate transaction;
    public CatalogImport(JdbcTemplate db, PlatformTransactionManager manager) {
        this.db = db; this.transaction = new TransactionTemplate(manager);
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        List<String[]> rows;
        try (var input = new ClassPathResource("catalog/recipes-uk.tsv").getInputStream()) {
            rows = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("\\t", -1)).toList();
        }
        var names = new HashSet<String>();
        for (var row : rows) {
            if (row.length != 7 || !names.add(row[0]) || Integer.parseInt(row[2]) < 1 || Integer.parseInt(row[3]) < 1
                    || row[4].isBlank() || row[6].split("\\|", -1).length < 3)
                throw new IllegalStateException("Invalid catalog recipe: " + row[0]);
            var ingredients = new HashSet<String>();
            for (String ingredient : row[5].split(";")) {
                var parts = ingredient.split("~", -1);
                if (parts.length != 3 || parts[0].isBlank() || !ingredients.add(parts[0])
                        || new BigDecimal(parts[1]).signum() <= 0
                        || !Set.of("г", "мл", "шт.", "ч. л.", "ст. л.").contains(parts[2]))
                    throw new IllegalStateException("Invalid ingredient in " + row[0]);
            }
            for (String step : row[6].split("\\|", -1))
                if (step.isBlank()) throw new IllegalStateException("Empty step: " + row[0]);
        }
        if (rows.size() != 90) throw new IllegalStateException("Expected exactly 90 catalog recipes");
        transaction.executeWithoutResult(status -> {
            db.execute("SELECT pg_advisory_xact_lock(768213409521)");
            var categories = namedValues("categories");
            var ingredients = namedValues("ingredients");
            var existing = new HashSet<>(db.queryForList("SELECT id FROM recipes", UUID.class));
            var recipeRows = new ArrayList<Object[]>();
            var linkRows = new ArrayList<Object[]>();
            var stepRows = new ArrayList<Object[]>();
            int inserted = 0;
            for (var row : rows) {
                UUID id = UUID.nameUUIDFromBytes(("lecook:catalog:uk:v1:" + row[0]).getBytes(StandardCharsets.UTF_8));
                if (existing.contains(id)) continue;
                int category = named("categories", row[1], categories);
                recipeRows.add(new Object[]{id, row[0], row[4], category, Integer.parseInt(row[2]), Integer.parseInt(row[3])});
                for (String ingredient : row[5].split(";")) {
                    var parts = ingredient.split("~");
                    linkRows.add(new Object[]{id, named("ingredients", parts[0], ingredients), new BigDecimal(parts[1]), parts[2]});
                }
                int number = 0;
                for (String step : row[6].split("\\|"))
                    stepRows.add(new Object[]{id, ++number, step});
                inserted++;
            }
            db.batchUpdate("INSERT INTO recipes(id,name,description,category_id,servings,preparation_minutes,created_at,updated_at) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", recipeRows);
            db.batchUpdate("INSERT INTO recipe_ingredients(recipe_id,ingredient_id,quantity,unit) VALUES (?,?,?,?)", linkRows);
            db.batchUpdate("INSERT INTO recipe_steps(recipe_id,step_number,instruction) VALUES (?,?,?)", stepRows);
            System.out.println("CATALOG IMPORT: inserted=" + inserted + ", skipped=" + (rows.size()-inserted) + ", dataset=" + rows.size());
            System.out.println("CATALOG TOTALS: recipes=" + db.queryForObject("SELECT count(*) FROM recipes", Long.class)
                + ", ingredients=" + db.queryForObject("SELECT count(*) FROM ingredients", Long.class)
                + ", links=" + db.queryForObject("SELECT count(*) FROM recipe_ingredients", Long.class)
                + ", steps=" + db.queryForObject("SELECT count(*) FROM recipe_steps", Long.class));
        });
    }
    private Map<String, Integer> namedValues(String table) {
        var result = new HashMap<String, Integer>();
        db.query("SELECT id,name FROM " + table + " ORDER BY id", rs -> {
            String name = rs.getString(2);
            if (name != null) result.putIfAbsent(name.trim().toLowerCase(Locale.ROOT), rs.getInt(1));
        });
        return result;
    }
    private int named(String table, String name, Map<String, Integer> values) {
        // table is one of the two internal literals above, never user input.
        return values.computeIfAbsent(name.trim().toLowerCase(Locale.ROOT), key ->
            db.queryForObject("INSERT INTO " + table + "(name) VALUES (?) RETURNING id", Integer.class, name));
    }
}
