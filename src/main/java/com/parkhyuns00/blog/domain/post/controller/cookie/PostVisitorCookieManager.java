package com.parkhyuns00.blog.domain.post.controller.cookie;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Component
public class PostVisitorCookieManager {

    public static final String COOKIE_NAME = "BLOG_VISITOR_ID";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final boolean secure;

    public PostVisitorCookieManager(@Value("${app.post-view.cookie.secure}") boolean secure) {
        this.secure = secure;
    }

    public UUID resolve(String cookieValue, HttpServletResponse response) {
        UUID visitorId = parseVisitorId(cookieValue);

        if (visitorId != null) return visitorId;

        UUID newVisitorId = UUID.randomUUID();

        addVisitorCookie(response, newVisitorId);

        return newVisitorId;
    }

    private UUID parseVisitorId(String cookieValue) {
        if (!StringUtils.hasText(cookieValue)) {
            return null;
        }

        try {
            return UUID.fromString(cookieValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void addVisitorCookie(HttpServletResponse response, UUID visitorId) {
        ResponseCookie cookie = ResponseCookie
            .from(COOKIE_NAME, visitorId.toString())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(COOKIE_MAX_AGE)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
