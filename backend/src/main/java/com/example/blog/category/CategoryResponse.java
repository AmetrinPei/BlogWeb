package com.example.blog.category;

public class CategoryResponse {

    private final Long id;
    private final String name;

    public CategoryResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
