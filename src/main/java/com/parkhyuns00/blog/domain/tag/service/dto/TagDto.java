package com.parkhyuns00.blog.domain.tag.service.dto;

import com.parkhyuns00.blog.domain.tag.model.Tag;

public record TagDto(
    Long tagId,
    String name,
    String slug
) {
    public static TagDto from(Tag tag) {
        return new TagDto(
            tag.getId(),
            tag.getName(),
            tag.getSlug()
        );
    }
}
