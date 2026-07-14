package com.example.blog.tag;

public class TagResponse {

    private final Long id;
    private final String name;

    public TagResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
