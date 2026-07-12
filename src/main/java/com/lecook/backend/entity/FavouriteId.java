package com.lecook.backend.entity;

import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FavouriteId {
    private UUID user_id;
    private UUID recipe_id;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FavouriteId that = (FavouriteId) o;
        return Objects.equals(user_id, that.user_id) && Objects.equals(recipe_id, that.recipe_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user_id, recipe_id);
    }
}
