package com.parkhyuns00.blog.domain.auth.controller.dto;

public record AdminAuthStatusResponse(
    boolean authenticated,
    AdminAuthStep step
) {
}
