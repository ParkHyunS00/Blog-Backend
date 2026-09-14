package com.parkhyuns00.blog.global.web.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

public class VisitorCookieManagerTest {

    @ParameterizedTest
    @ValueSource(strings = {"POST_VIEWER_ID", "VISITOR_ID"})
    @DisplayName("방문자 쿠키가 없으면 새로운 UUID 쿠키를 발급한다.")
    void test_resolve_issue_cookie_when_cookie_missing(String cookieName) {
        VisitorCookieManager cookieManager = new VisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve(cookieName, null, response);
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(visitorId).isNotNull();
        assertThat(setCookie)
            .contains(
                cookieName + "=" + visitorId,
                "Max-Age=31536000",
                "Path=/",
                "HttpOnly",
                "SameSite=Lax"
            )
            .doesNotContain("Secure");
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST_VIEWER_ID", "VISITOR_ID"})
    @DisplayName("정상적인 방문자 쿠키가 있으면 기존 UUID를 재사용한다.")
    void test_resolve_reuse_existing_cookie(String cookieName) {
        VisitorCookieManager cookieManager = new VisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UUID existingVisitorId = UUID.randomUUID();

        UUID visitorId = cookieManager.resolve(cookieName, existingVisitorId.toString(), response);

        assertThat(visitorId).isEqualTo(existingVisitorId);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST_VIEWER_ID", "VISITOR_ID"})
    @DisplayName("방문자 쿠키가 올바른 UUID가 아니면 새로운 쿠키로 교체한다.")
    void test_resolve_replace_cookie_when_cookie_invalid(String cookieName) {
        VisitorCookieManager cookieManager = new VisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve(cookieName, "invalid-visitor-id", response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(visitorId).isNotNull();
        assertThat(setCookie).contains(cookieName + "=" + visitorId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST_VIEWER_ID", "VISITOR_ID"})
    @DisplayName("운영 환경에서는 방문자 쿠키에 Secure 속성을 추가한다.")
    void test_resolve_issue_secure_cookie(String cookieName) {
        VisitorCookieManager cookieManager = new VisitorCookieManager(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve(cookieName, null, response);

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains(
                cookieName + "=" + visitorId,
                "Secure"
            );
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST_VIEWER_ID", "VISITOR_ID"})
    @DisplayName("방문자 쿠키가 공백이면 새로운 쿠키를 발급한다.")
    void test_resolve_issue_cookie_when_cookie_blank(String cookieName) {
        VisitorCookieManager cookieManager = new VisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UUID visitorId = cookieManager.resolve(cookieName, " ", response);

        assertThat(visitorId).isNotNull();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
            .contains(cookieName + "=" + visitorId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST_VIEWER_ID", "VISITOR_ID"})
    @DisplayName("쿠키를 발급할 때 기존 Set-Cookie 헤더를 유지한다.")
    void test_resolve_preserve_existing_cookie_header(String cookieName) {
        VisitorCookieManager cookieManager = new VisitorCookieManager(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.addHeader(HttpHeaders.SET_COOKIE, "OTHER_COOKIE=value; Path=/");

        UUID visitorId = cookieManager.resolve(cookieName, null, response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
            .hasSize(2)
            .contains("OTHER_COOKIE=value; Path=/");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
            .anySatisfy(header ->
                assertThat(header).startsWith(cookieName + "=" + visitorId + ";")
            );
    }
}
