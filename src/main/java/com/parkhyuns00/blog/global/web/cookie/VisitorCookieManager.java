package com.parkhyuns00.blog.global.web.cookie;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Component("commonVisitorCookieManager")
public class VisitorCookieManager {

    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final boolean secure;

    public VisitorCookieManager(@Value("${app.visitor.cookie.secure}") boolean secure) {
        this.secure = secure;
    }

    public UUID resolve(String cookieName, String cookieValue, HttpServletResponse response) {
        UUID visitorId = parseVisitorId(cookieValue);

        if (visitorId != null) {
            return visitorId;
        }

        UUID newVisitorId = UUID.randomUUID();

        addVisitorCookie(cookieName, newVisitorId, response);

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

    private void addVisitorCookie(String cookieName, UUID visitorId, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
            .from(cookieName, visitorId.toString())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(COOKIE_MAX_AGE)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
