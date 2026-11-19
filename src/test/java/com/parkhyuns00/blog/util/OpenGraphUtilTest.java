package com.parkhyuns00.blog.util;

import com.parkhyuns00.blog.config.cors.dto.CorsProperties;
import com.parkhyuns00.blog.domain.post.service.dto.PostOpenGraphDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenGraphUtilTest {

    private final OpenGraphUtil openGraphUtil = new OpenGraphUtil(
        new CorsProperties(List.of("https://blog.parkhyuns00.com"))
    );

    @Test
    @DisplayName("기존 CORS 첫 origin의 마지막 슬래시를 정리해 모든 공개 URL에 사용한다.")
    void test_first_configured_origin_is_used_for_public_urls() {
        OpenGraphUtil configuredUtil = new OpenGraphUtil(new CorsProperties(List.of(
            "https://preview.example.test/", "https://secondary.example.test"
        )));

        String thumbnailHtml = configuredUtil.render(
            new PostOpenGraphDto(42L, "제목", "요약", 2L, "image/webp", 800, 450));
        String defaultImageHtml = configuredUtil.render(
            new PostOpenGraphDto(42L, "제목", "요약", null, null, null, null));

        for (String html : List.of(thumbnailHtml, defaultImageHtml)) {
            assertThat(html)
                .contains("<meta property=\"og:url\" content=\"https://preview.example.test/posts/42\">")
                .contains("<a href=\"https://preview.example.test/posts/42\">")
                .doesNotContain("secondary.example.test", "preview.example.test//");
        }
        assertThat(thumbnailHtml)
            .contains("<meta property=\"og:image\" content=\"https://preview.example.test/api/post-images/2\">");
        assertThat(defaultImageHtml)
            .contains("<meta property=\"og:image\" content=\"https://preview.example.test/og-default.png\">");
    }

    @Test
    @DisplayName("제목과 요약을 HTML 텍스트와 속성 모두에서 안전하게 이스케이프한다.")
    void test_escape_dynamic_text_and_attributes() {
        String html = openGraphUtil.render(new PostOpenGraphDto(
            1L, "제목 \"' & <script>alert(1)</script>",
            "요약 \"' & <img src=x onerror=alert(1)>", null, null, null, null
        ));

        assertThat(html)
            .contains("<title>제목 &quot;&#39; &amp; &lt;script&gt;alert(1)&lt;/script&gt; | blog.parkhyuns00.com</title>")
            .contains("<meta property=\"og:title\" content=\"제목 &quot;&#39; &amp; &lt;script&gt;alert(1)&lt;/script&gt;\">")
            .contains("<meta property=\"og:image:alt\" content=\"제목 &quot;&#39; &amp; &lt;script&gt;alert(1)&lt;/script&gt;\">")
            .contains("<meta name=\"description\" content=\"요약 &quot;&#39; &amp; &lt;img src=x onerror=alert(1)&gt;\">")
            .contains("<meta property=\"og:description\" content=\"요약 &quot;&#39; &amp; &lt;img src=x onerror=alert(1)&gt;\">")
            .doesNotContain("<script", "<img", "http-equiv", "/api/posts/og/");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("null 또는 공백 요약은 기본 소개 문구로 표시한다.")
    void test_fallback_description(String summary) {
        String html = openGraphUtil.render(new PostOpenGraphDto(1L, "제목", summary, null, null, null, null));

        assertThat(html)
            .contains("<meta name=\"description\" content=\"현수의 성장 아카이브\">")
            .contains("<meta property=\"og:description\" content=\"현수의 성장 아카이브\">");
    }

    @Test
    @DisplayName("알 수 없는 썸네일 MIME 및 크기는 기본 이미지 정보로 대체하지 않고 생략한다.")
    void test_unknown_thumbnail_metadata_is_omitted() {
        String html = openGraphUtil.render(new PostOpenGraphDto(1L, "제목", "요약", 2L, null, null, null));

        assertThat(html)
            .contains("<meta property=\"og:image\" content=\"https://blog.parkhyuns00.com/api/post-images/2\">")
            .doesNotContain("og:image:type", "og:image:width", "og:image:height", "1200", "630");
    }

    @Test
    @DisplayName("양수가 아닌 이미지 크기는 출력하지 않는다.")
    void test_invalid_thumbnail_dimensions_are_omitted() {
        String html = openGraphUtil.render(new PostOpenGraphDto(1L, "제목", "요약", 2L, "image/webp", 0, -1));

        assertThat(html).contains("<meta property=\"og:image:type\" content=\"image/webp\">")
            .doesNotContain("og:image:width", "og:image:height");
    }

}
