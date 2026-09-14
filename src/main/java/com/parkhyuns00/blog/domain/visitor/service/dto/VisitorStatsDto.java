package com.parkhyuns00.blog.domain.visitor.service.dto;

public record VisitorStatsDto(
    long total,
    long today,
    long yesterday
) {
}
