package com.parkhyuns00.blog.config.security.filter.dto;

public record AdminOtpLoginRequest(
    String otpCode
) {}
