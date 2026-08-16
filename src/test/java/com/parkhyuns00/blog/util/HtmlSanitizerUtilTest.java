package com.parkhyuns00.blog.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HtmlSanitizerUtilTest {

    private final HtmlSanitizerUtil htmlSanitizerUtil = new HtmlSanitizerUtil();

    @Test
    @DisplayName("허용된 HTML 태그는 유지한다.")
    void test_sanitize_allowed_html_success() {
        String html = """
              <h2>제목</h2>
              <p><strong>강조된 내용</strong></p>
              <blockquote>인용문</blockquote>
              <ul><li>목록</li></ul>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<h2>제목</h2>")
            .contains("<p><strong>강조된 내용</strong></p>")
            .contains("<blockquote>인용문</blockquote>")
            .contains("<ul><li>목록</li></ul>");
    }

    @Test
    @DisplayName("script 태그와 내부 스크립트를 제거한다.")
    void test_sanitize_remove_script_element() {
        String html = """
              <p>정상 내용</p>
              <script>alert('xss')</script>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<p>정상 내용</p>")
            .doesNotContain("<script")
            .doesNotContain("alert('xss')");
    }

    @Test
    @DisplayName("iframe 등 허용되지 않은 태그를 제거한다.")
    void test_sanitize_remove_disallowed_elements() {
        String html = """
              <p>정상 내용</p>
              <iframe src="https://example"></iframe>
              <object data="https://example"></object>
              <form><input type="text"></form>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<p>정상 내용</p>")
            .doesNotContain("<iframe")
            .doesNotContain("<object")
            .doesNotContain("<form")
            .doesNotContain("<input");
    }

    @Test
    @DisplayName("HTML 이벤트 속성을 제거한다.")
    void test_sanitize_remove_event_attributes() {
        String html = """
              <p onclick="alert('xss')">본문</p>
              <img src="/api/post-images/1"
                   alt="이미지"
                   onerror="alert('xss')">
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("본문")
            .contains("/api/post-images/1")
            .doesNotContain("onclick")
            .doesNotContain("onerror")
            .doesNotContain("alert('xss')");
    }

    @Test
    @DisplayName("HTTP와 HTTPS 요청을 허용한다.")
    void test_sanitize_allow_http_and_https_links() {
        String html = """
              <a href="http://example.com">HTTP</a>
              <a href="https://example.com">HTTPS</a>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("href=\"http://example.com\"")
            .contains("href=\"https://example.com\"");
    }

    @Test
    @DisplayName("javascript 프로토콜 링크를 제거한다.")
    void test_sanitize_remove_javascript_link() {
        String html = "<a href=\"javascript:alert('xss')\">위험한 링크</a>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("위험한 링크")
            .doesNotContain("javascript:")
            .doesNotContain("href=");
    }

    @Test
    @DisplayName("게시글 이미지 API 경로를 이미지 주소로 허용한다.")
    void test_sanitize_allow_post_image_path() {
        String html = "<img src=\"/api/post-images/123\" alt=\"게시글 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("src=\"/api/post-images/123\"")
            .contains("alt=\"게시글 이미지\"");
    }

    @Test
    @DisplayName("외부 이미지 주소를 제거한다.")
    void test_sanitize_remove_external_image_source() {
        String html = "<img src=\"https://evil.example/image.png\" alt=\"외부 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("https://evil.example/image.png")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("data 이미지 주소를 제거한다.")
    void test_sanitize_remove_data_image_source() {
        String html = "<img src=\"data:image/png;base64,AAAA\" alt=\"인라인 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("data:image")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("blob 이미지 주소를 제거한다.")
    void test_sanitize_remove_blob_image_source() {
        String html = "<img src=\"blob:https://example.com/image\" alt=\"Blob 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("blob:")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("양수가 아닌 게시글 이미지 ID 경로를 제거한다.")
    void test_sanitize_remove_invalid_post_image_path() {
        String html = "<img src=\"/api/post-images/0\" alt=\"잘못된 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("/api/post-images/0")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("올바른 코드 언어 클래스는 유지한다.")
    void test_sanitize_allow_valid_code_class() {
        String html = "<pre><code class=\"language-kotlin\">val name = \"blog\"</code></pre>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("class=\"language-kotlin\"")
            .contains("val name")
            .contains("blog");
    }

    @Test
    @DisplayName("허용된 형식이 아닌 코드 클래스는 제거한다.")
    void test_sanitize_remove_invalid_code_class() {
        String html = "<pre><code class=\"language-java malicious\">code</code></pre>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("code")
            .doesNotContain("class=");
    }

    @Test
    @DisplayName("HEX 형식의 형광펜 색상을 소문자로 정규화한다.")
    void test_sanitize_allow_hex_mark_color() {
        String html = "<mark data-color=\"#FDE047\">강조</mark>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("data-color=\"#fde047\"")
            .contains(">강조</mark>");
    }

    @Test
    @DisplayName("HEX 형식이 아닌 형광펜 색상을 제거한다.")
    void test_sanitize_remove_invalid_mark_color() {
        String html = "<mark data-color=\"red\">강조</mark>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<mark>강조</mark>")
            .doesNotContain("data-color");
    }

    @Test
    @DisplayName("style 속성을 제거한다.")
    void test_sanitize_remove_style_attribute() {
        String html = """
              <p style="color: red">본문</p>
              <mark
                  data-color="#fde047"
                  style="background-image: url(javascript:alert('xss'))">
                  강조
              </mark>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("본문")
            .contains("data-color=\"#fde047\"")
            .doesNotContain("style=")
            .doesNotContain("javascript:");
    }

    @Test
    @DisplayName("null HTML을 입력하면 null을 반환한다.")
    void test_sanitize_return_null_when_html_null() {
        String result = htmlSanitizerUtil.sanitize(null);

        assertThat(result).isNull();
    }
}
