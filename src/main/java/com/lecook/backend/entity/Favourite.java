//package com.lecook.backend.entity;
//
//import jakarta.persistence.*;
//
//@Entity
//@Table(name = "favourites")
//public class Favourite {
//
//    @EmbeddedId
//    private FavouriteId id;
//
//    @ManyToOne
//    @MapsId("recipeId")
//    @JoinColumn(name = "recipe_id")
//    private Recipe recipe;
//
//    @ManyToOne
//    @MapsId("ingredientId")
//    @JoinColumn(name = "ingredient_id")
//    private Ingredient ingredient;
//}
//