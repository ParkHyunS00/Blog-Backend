package com.parkhyuns00.blog.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.post.controller.cookie.PostVisitorCookieManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

public class PostVisitorCookieManagerTest {

    @Test
    @DisplayName("방문자 쿠키가 없으면 새로운 UUID 쿠키를 발급한다.")
    void test_resolve_issue_cookie_when_cookie_missing() {
        PostVisitorCookieManager cookieManager = new PostVisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve(null, response);
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(visitorId).isNotNull();
        assertThat(setCookie)
            .contains(
                "BLOG_VISITOR_ID=" + visitorId,
                "Max-Age=31536000",
                "Path=/",
                "HttpOnly",
                "SameSite=Lax"
            )
            .doesNotContain("Secure");
    }

    @Test
    @DisplayName("정상적인 방문자 쿠키가 있으면 기존 UUID를 재사용한다.")
    void test_resolve_reuse_existing_cookie() {
        PostVisitorCookieManager cookieManager = new PostVisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UUID existingVisitorId = UUID.randomUUID();

        UUID visitorId = cookieManager.resolve(existingVisitorId.toString(), response);

        assertThat(visitorId).isEqualTo(existingVisitorId);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("방문자 쿠키가 올바른 UUID가 아니면 새로운 쿠키로 교체한다.")
    void test_resolve_replace_cookie_when_cookie_invalid() {
        PostVisitorCookieManager cookieManager = new PostVisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve("invalid-visitor-id", response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(visitorId).isNotNull();
        assertThat(setCookie).contains("BLOG_VISITOR_ID=" + visitorId);
    }

    @Test
    @DisplayName("운영 환경에서는 방문자 쿠키에 Secure 속성을 추가한다.")
    void test_resolve_issue_secure_cookie() {
        PostVisitorCookieManager cookieManager = new PostVisitorCookieManager(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve(null, response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains(
                "BLOG_VISITOR_ID=" + visitorId,
                "Secure"
            );
    }
}
