package com.parkhyuns00.blog.config.security.handler.dto;

import com.parkhyuns00.blog.config.security.filter.dto.AdminAuthStatus;

public record AdminAuthResponse(
    AdminAuthStatus message
) {
}
