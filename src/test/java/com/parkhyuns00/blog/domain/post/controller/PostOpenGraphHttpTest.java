package com.parkhyuns00.blog.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.PostOpenGraphDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.cors.allowed-origins=https://blog.parkhyuns00.com"
)
@ActiveProfiles("test")
class PostOpenGraphHttpTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    @LocalServerPort
    private int port;

    @MockitoBean
    private PostService postService;

    @AfterAll
    static void closeClient() {
        HTTP_CLIENT.close();
    }

    @Test
    @DisplayName("실제 HTTP 익명 GET은 HTML을 반환하고 HEAD는 같은 헤더와 빈 본문을 반환한다.")
    void test_public_get_and_head_over_http() throws Exception {
        when(postService.getPublishedPostOpenGraph(7L)).thenReturn(new PostOpenGraphDto(
            7L, "실제 HTTP 게시글", "크롤러용 요약", 19L, "image/jpeg", 800, 450
        ));

        HttpResponse<byte[]> getResponse = request("GET", "7");
        HttpResponse<byte[]> headResponse = request("HEAD", "7");

        assertThat(getResponse.statusCode()).isEqualTo(200);
        MediaType contentType = MediaType.parseMediaType(
            getResponse.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElseThrow());
        assertThat(contentType.isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
        String html = new String(getResponse.body(), StandardCharsets.UTF_8);
        assertThat(html).startsWith("<!DOCTYPE html>")
            .contains("<title>실제 HTTP 게시글 | blog.parkhyuns00.com</title>")
            .contains("property=\"og:description\" content=\"크롤러용 요약\"")
            .contains("property=\"og:url\" content=\"https://blog.parkhyuns00.com/posts/7\"")
            .contains("property=\"og:image\" content=\"https://blog.parkhyuns00.com/api/post-images/19\"")
            .doesNotContain("\"data\":");
        assertThat(getResponse.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
        assertThat(getResponse.headers().allValues(HttpHeaders.LOCATION)).isEmpty();
        assertHeadMatches(getResponse, headResponse);
        verify(postService, times(2)).getPublishedPostOpenGraph(7L);
    }

    @Test
    @DisplayName("HTML만 수락하는 크롤러도 미존재 글의 GET과 HEAD에서 404를 받는다.")
    void test_missing_post_get_and_head_over_http() throws Exception {
        when(postService.getPublishedPostOpenGraph(8L))
            .thenThrow(new PostException(PostExceptionCode.POST_NOT_FOUND));

        HttpResponse<byte[]> getResponse = request("GET", "8");
        HttpResponse<byte[]> headResponse = request("HEAD", "8");

        assertThat(getResponse.statusCode()).isEqualTo(404);
        assertHeadMatches(getResponse, headResponse);
        verify(postService, times(2)).getPublishedPostOpenGraph(8L);
    }

    @Test
    @DisplayName("조회 오류는 실제 HTTP GET과 HEAD에서도 정상 OG로 숨기지 않고 500을 반환한다.")
    void test_query_failure_get_and_head_over_http() throws Exception {
        when(postService.getPublishedPostOpenGraph(9L))
            .thenThrow(new IllegalStateException("database unavailable"));

        HttpResponse<byte[]> getResponse = request("GET", "9");
        HttpResponse<byte[]> headResponse = request("HEAD", "9");

        assertThat(getResponse.statusCode()).isEqualTo(500);
        assertHeadMatches(getResponse, headResponse);
        verify(postService, times(2)).getPublishedPostOpenGraph(9L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "1.5", "1e3", "9223372036854775808"})
    @DisplayName("양의 Long 정수가 아닌 postId는 실제 HTTP GET과 HEAD에서 조회 없이 400을 반환한다.")
    void test_invalid_post_id_get_and_head_over_http(String postId) throws Exception {
        HttpResponse<byte[]> getResponse = request("GET", postId);
        HttpResponse<byte[]> headResponse = request("HEAD", postId);

        assertThat(getResponse.statusCode()).isEqualTo(400);
        assertHeadMatches(getResponse, headResponse);
        verifyNoInteractions(postService);
    }

    private HttpResponse<byte[]> request(String method, String postId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/posts/og/" + postId))
            .header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
            .timeout(Duration.ofSeconds(5))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private void assertHeadMatches(HttpResponse<byte[]> getResponse, HttpResponse<byte[]> headResponse) {
        assertThat(headResponse.statusCode()).isEqualTo(getResponse.statusCode());
        assertThat(headResponse.body()).isEmpty();
        assertThat(getResponse.headers().firstValue(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        for (String header : List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.CACHE_CONTROL, HttpHeaders.CONTENT_LENGTH)) {
            assertThat(headResponse.headers().allValues(header))
                .as("GET and HEAD %s", header).isEqualTo(getResponse.headers().allValues(header));
        }
        assertThat(headResponse.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
    }
}
