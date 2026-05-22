package com.parkhyuns00.blog.util;

import org.springframework.stereotype.Component;

@Component
public class SlugUtil {

    private static final String SLUG_SEPARATOR = "-";
    private static final String INVALID_SLUG_CHARACTERS_REGEX = "[^a-z0-9가-힣]+";
    private static final String DUPLICATED_SEPARATOR_REGEX = "-+";
    private static final String EDGE_SEPARATOR_REGEX = "^-|-$";

    public String generate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Slug source must not be blank.");
        }

        String slug = value.trim()
            .toLowerCase()
            .replaceAll(INVALID_SLUG_CHARACTERS_REGEX, SLUG_SEPARATOR)
            .replaceAll(DUPLICATED_SEPARATOR_REGEX, SLUG_SEPARATOR)
            .replaceAll(EDGE_SEPARATOR_REGEX, "");

        if (slug.isBlank()) {
            throw new IllegalArgumentException("Slug must not be blank.");
        }

        return slug;
    }
}
