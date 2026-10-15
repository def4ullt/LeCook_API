package com.lecook.backend.config;

import com.lecook.backend.dto.RecipeRequest;
import com.lecook.backend.entity.Category;
import com.lecook.backend.repository.CategoryRepository;
import com.lecook.backend.repository.RecipeRepository;
import com.lecook.backend.service.RecipeService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit opt-in sample catalog, only when the recipe table is empty. */
@Component
@Profile("demo")
public class DemoCatalog implements ApplicationRunner {
    private final RecipeRepository recipes;
    private final CategoryRepository categories;
    private final RecipeService service;
    public DemoCatalog(RecipeRepository recipes, CategoryRepository categories, RecipeService service) {
        this.recipes = recipes; this.categories = categories; this.service = service;
    }
    @Override @Transactional
    public void run(ApplicationArguments arguments) {
        if (recipes.count() != 0) return;
        add("Омлет із зеленню", "Сніданок", 280, "Інгредієнти на 1 порцію:\n2 яйця, 30 мл молока, зелень, сіль, трохи олії.\n\n1. Збийте яйця з молоком і сіллю.\n2. Вилийте на розігріту сковороду з олією.\n3. Готуйте під кришкою до готовності. Додайте зелень.");
        add("Паста з томатами", "Вечеря", 420, "Інгредієнти на 1 порцію:\n100 г сухої пасти, 150 г томатів, зубчик часнику, оливкова олія, базилік, сіль.\n\n1. Відваріть пасту за інструкцією на упаковці.\n2. Коротко обсмажте подрібнений часник на олії.\n3. Додайте томати й тушкуйте 10 хвилин.\n4. Змішайте з пастою та базиліком.");
        add("Йогурт із ягодами", "Десерт", 190, "Інгредієнти на 1 порцію:\n150 г натурального йогурту, 80 г ягід, 15 г вівсяних пластівців.\n\n1. Вимийте ягоди.\n2. Викладіть йогурт у миску.\n3. Додайте ягоди й пластівці та перемішайте.");
    }
    private void add(String name, String categoryName, int calories, String description) {
        var category = categories.findAll().stream().filter(item -> categoryName.equals(item.getName())).findFirst().orElseGet(() -> {
            var item = new Category(); item.setName(categoryName); return categories.saveAndFlush(item);
        });
        service.create(new RecipeRequest(name, description, calories, category.getId(), null));
    }
}
