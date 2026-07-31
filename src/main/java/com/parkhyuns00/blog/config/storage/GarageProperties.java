package com.parkhyuns00.blog.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record GarageProperties(
    String endpoint,
    String bucket,
    String accessKey,
    String secretKey
) {
}
