package com.lecook.backend.repository;

import com.lecook.backend.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {
    @org.springframework.data.jpa.repository.Query("select r from Recipe r where r.ID not in :ids")
    java.util.List<Recipe> findUnknown(@org.springframework.data.repository.query.Param("ids") java.util.List<UUID> ids);

    @org.springframework.data.jpa.repository.Query("select r from Recipe r where r.ID not in :ids order by r.ID")
    java.util.List<Recipe> findUnknownPage(@org.springframework.data.repository.query.Param("ids") java.util.List<UUID> ids, org.springframework.data.domain.Pageable pageable);
}
