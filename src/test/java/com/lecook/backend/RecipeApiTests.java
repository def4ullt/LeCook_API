package com.lecook.backend;

import com.lecook.backend.controller.*;
import com.lecook.backend.dto.*;
import com.lecook.backend.entity.*;
import com.lecook.backend.exception.ApiExceptionHandler;
import com.lecook.backend.repository.*;
import com.lecook.backend.service.RecipeSyncService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class RecipeApiTests extends IsolatedDatabase {
    @Autowired RecipeController recipes;
    @Autowired CategoryController categories;
    @Autowired RecipeSyncController sync;
    @Autowired RecipeIngredientController recipeIngredients;
    @Autowired RecipeSyncService syncService;
    @Autowired RecipeRepository repository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired RecipeIngredientRepository ingredientLinks;
    @Autowired IngredientRepository ingredients;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(recipes, categories, sync, recipeIngredients).setControllerAdvice(new ApiExceptionHandler()).build();
    }
    private Category category() {
        var category = new Category(); category.setName("Обід"); return categoryRepository.saveAndFlush(category);
    }
    private String request(Integer categoryId, String name) throws Exception {
        return mapper.writeValueAsString(new RecipeRequest(name, "Буряк, картопля. Варити до готовності.", 200, categoryId, null));
    }

    @Test void recipeCrudReturnsDtosAndDatesAndHandlesMissingRecords() throws Exception {
        var category = category();
        var created = mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(request(category.getId(), "Борщ")))
            .andExpect(status().isCreated()).andReturn().getResponse();
        var body = mapper.readTree(created.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var id = body.get("id").asText();
        assertEquals("/api/recipes/" + id, created.getHeader("Location"));
        assertEquals("Обід", body.get("category").asText());
        assertFalse(body.get("createdAt").isNull());
        assertFalse(body.get("updatedAt").isNull());
        mvc.perform(get("/api/recipes/" + id)).andExpect(status().isOk());
        mvc.perform(put("/api/recipes/" + id).contentType(MediaType.APPLICATION_JSON).content(request(category.getId(), "Зелений борщ"))).andExpect(status().isOk());
        assertEquals("Зелений борщ", repository.findById(UUID.fromString(id)).orElseThrow().getName());
        mvc.perform(delete("/api/recipes/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/recipes/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/recipes/" + id)).andExpect(status().isNotFound());
    }

    @Test void rejectsInvalidBodiesMissingCategoriesAndMalformedIds() throws Exception {
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/recipes").contentType(MediaType.APPLICATION_JSON).content(request(Integer.MAX_VALUE, "Борщ")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/recipes/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/recipes?size=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/recipes?size=201")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/recipes/sync/page").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void syncPaginatesWithoutDuplicatesAndStopsWhenKnown() {
        var category = category();
        for (int index = 0; index < 205; index++) {
            var recipe = new Recipe(); recipe.setName("Страва " + index); recipe.setDescription("Кроки приготування");
            recipe.setCalories(100); recipe.setCategory(category); repository.save(recipe);
        }
        repository.flush();
        var first = syncService.page(new SyncRequest(List.of()));
        assertEquals(200, first.recipes().size()); assertTrue(first.hasMore());
        var known = new ArrayList<>(first.recipes().stream().map(DownloadRecipe::id).toList());
        var second = syncService.page(new SyncRequest(known));
        assertEquals(5, second.recipes().size()); assertFalse(second.hasMore());
        assertTrue(second.recipes().stream().noneMatch(recipe -> known.contains(recipe.id())));
        known.addAll(second.recipes().stream().map(DownloadRecipe::id).toList());
        assertTrue(syncService.page(new SyncRequest(known)).recipes().isEmpty());
    }

    @Test void deletionAlsoRemovesIngredientLinks() {
        var recipe = new Recipe(); recipe.setName("Суп"); recipe.setDescription("Кроки"); recipe.setCategory(category());
        repository.saveAndFlush(recipe);
        var ingredient = new Ingredient(); ingredient.setName("Вода"); ingredients.saveAndFlush(ingredient);
        var link = new RecipeIngredient();
        link.setRecipe(recipe); link.setIngredient(ingredient);
        link.setId(new RecipeIngredientId(recipe.getID(), ingredient.getId()));
        link.setQuantity(java.math.BigDecimal.ONE);
        ingredientLinks.saveAndFlush(link);
        recipes.delete(recipe.getID());
        assertFalse(repository.existsById(recipe.getID()));
        assertFalse(ingredientLinks.existsById(link.getId()));
        assertTrue(ingredients.existsById(ingredient.getId()));
    }

    @Test void ingredientEndpointsValidateAndUseBothIds() throws Exception {
        var recipe = new Recipe(); recipe.setName("Суп"); recipe.setDescription("Кроки"); recipe.setCategory(category());
        repository.saveAndFlush(recipe);
        var ingredient = new Ingredient(); ingredient.setName("Вода"); ingredients.saveAndFlush(ingredient);
        var path = "/api/recipes/" + recipe.getID() + "/ingredients";
        var payload = mapper.writeValueAsString(new RecipeIngredientRequest(ingredient.getId(), java.math.BigDecimal.ONE));
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
        mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(put(path + "/" + ingredient.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(put(path + "/" + ingredient.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2.5}")).andExpect(status().isOk());
        mvc.perform(delete(path + "/" + ingredient.getId())).andExpect(status().isNoContent());
        mvc.perform(delete(path + "/" + ingredient.getId())).andExpect(status().isNotFound());
    }
}
