package com.parkhyuns00.blog.config.security.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin")
public record AdminProperties(
    String accessKeyHash,
    String otpSecret
) {
}
