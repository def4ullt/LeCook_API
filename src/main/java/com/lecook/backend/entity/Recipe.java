package com.lecook.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipes")
@Data
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ID;

    private String name;
    @Column(columnDefinition = "text")
    private String description;

    private String image_url;
    private Integer calories;
    private Double protein;
    private Double fat;
    private Double carbs;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void created() {
        createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() { updatedAt = LocalDateTime.now(java.time.ZoneOffset.UTC); }
}
