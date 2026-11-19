package com.parkhyuns00.blog.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.util.GarageUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest(properties = "app.cors.allowed-origins=https://blog.parkhyuns00.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostOpenGraphIntegrationTest {

    private static final String SITE_ORIGIN = "https://blog.parkhyuns00.com";
    private static final Pattern META_TAG = Pattern.compile("<meta\\s+([^>]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)=\"([^\"]*)\"");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private GarageUtil garageUtil;

    @Test
    @DisplayName("익명 요청에 공개 게시글과 실제 썸네일 메타데이터를 HTML로 반환한다.")
    void test_public_post_returns_html_without_authentication_or_json_wrapper() throws Exception {
        Post post = publishedPost();
        PostImage thumbnail = thumbnail(post);
        flushAndClear();

        MockHttpServletResponse response = mockMvc.perform(get("/api/posts/og/{postId}", post.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().encoding(StandardCharsets.UTF_8.name()))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
            .andReturn().getResponse();

        String html = response.getContentAsString();
        assertThat(html).startsWith("<!DOCTYPE html>")
            .contains("<html lang=\"ko\">", "<head>", "<body>", "</html>")
            .contains("<title>공개 게시글 | blog.parkhyuns00.com</title>")
            .contains("href=\"" + SITE_ORIGIN + "/posts/" + post.getId() + "\"")
            .doesNotContain("\"data\":", "<script", "http-equiv=\"refresh\"");
        assertThat(meta(html, "description")).isEqualTo("공개 요약");
        assertThat(meta(html, "og:type")).isEqualTo("article");
        assertThat(meta(html, "og:site_name")).isEqualTo("blog.parkhyuns00.com");
        assertThat(meta(html, "og:title")).isEqualTo("공개 게시글");
        assertThat(meta(html, "og:description")).isEqualTo("공개 요약");
        assertThat(meta(html, "og:url")).isEqualTo(SITE_ORIGIN + "/posts/" + post.getId());
        assertThat(meta(html, "og:image")).isEqualTo(SITE_ORIGIN + "/api/post-images/" + thumbnail.getId());
        assertThat(meta(html, "og:image:alt")).contains("공개 게시글");
        assertThat(meta(html, "og:image:type")).isEqualTo("image/webp");
        assertThat(meta(html, "og:image:width")).isEqualTo("960");
        assertThat(meta(html, "og:image:height")).isEqualTo("480");
        assertThat(meta(html, "og:locale")).isEqualTo("ko_KR");
        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("썸네일이 없는 글은 기본 PNG 이미지와 실제 기본 이미지 크기를 사용한다.")
    void test_post_without_thumbnail_uses_default_image() throws Exception {
        Post post = publishedPost();
        flushAndClear();

        String html = getHtml(post.getId());

        assertDefaultImage(html);
        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("연결이 해제되거나 삭제된 썸네일과 본문 이미지는 OG 썸네일로 사용하지 않는다.")
    void test_detached_deleted_or_content_image_does_not_replace_default() throws Exception {
        Post post = publishedPost();
        PostImage detached = thumbnail(post);
        detached.detach();
        PostImage removed = thumbnail(post);
        postImageRepository.delete(removed);
        PostImage contentImage = new PostImage(PostImageType.CONTENT, "posts/content/body.png", "image/png", 500, 500);
        contentImage.attachTo(post);
        postImageRepository.save(contentImage);
        flushAndClear();

        assertDefaultImage(getHtml(post.getId()));
        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("저장된 요약이 공백이면 기본 소개 문구를 사용한다.")
    void test_blank_stored_summary_uses_default_description() throws Exception {
        Post post = publishedPost();
        ReflectionTestUtils.setField(post, "summary", " \t\n ");
        flushAndClear();

        String html = getHtml(post.getId());

        assertThat(meta(html, "description")).isEqualTo("현수의 성장 아카이브");
        assertThat(meta(html, "og:description")).isEqualTo("현수의 성장 아카이브");
    }

    @Test
    @DisplayName("임시저장 글은 관리자 요청에서도 공개하지 않고 GET과 HEAD에 404를 반환한다.")
    void test_draft_is_not_public_even_to_admin() throws Exception {
        Post draft = postRepository.save(Post.createDraft("비공개 초안", "초안 요약", "초안 본문", null));
        flushAndClear();

        mockMvc.perform(get("/api/posts/og/{postId}", draft.getId()))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/posts/og/{postId}", draft.getId()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound());
        mockMvc.perform(head("/api/posts/og/{postId}", draft.getId()).with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound())
            .andExpect(content().bytes(new byte[0]));
    }

    @Test
    @DisplayName("없는 글과 삭제된 글은 GET과 HEAD에 같은 404 상태와 헤더를 반환한다.")
    void test_missing_and_deleted_post_return_404_for_get_and_head() throws Exception {
        Post post = publishedPost();
        Long deletedId = post.getId();
        postRepository.delete(post);
        flushAndClear();

        for (Long postId : List.of(deletedId, Long.MAX_VALUE)) {
            MockHttpServletResponse getResponse = mockMvc.perform(get("/api/posts/og/{postId}", postId))
                .andExpect(status().isNotFound())
                .andReturn().getResponse();
            MockHttpServletResponse headResponse = mockMvc.perform(head("/api/posts/og/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(content().bytes(new byte[0]))
                .andReturn().getResponse();

            assertSameHeaders(getResponse, headResponse);
        }
    }

    @Test
    @DisplayName("익명 HEAD는 GET과 같은 상태 및 헤더를 반환하고 조회수 부수 효과가 없다.")
    void test_get_and_head_leave_view_count_unchanged() throws Exception {
        Post post = publishedPost();
        ReflectionTestUtils.setField(post, "viewCount", 41L);
        flushAndClear();

        MockHttpServletResponse getResponse = mockMvc.perform(get("/api/posts/og/{postId}", post.getId()))
            .andExpect(status().isOk())
            .andReturn().getResponse();
        MockHttpServletResponse headResponse = mockMvc.perform(head("/api/posts/og/{postId}", post.getId()))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
            .andReturn().getResponse();

        assertSameHeaders(getResponse, headResponse);
        flushAndClear();
        assertThat(postRepository.findById(post.getId()).orElseThrow().getViewCount()).isEqualTo(41L);
        verifyNoInteractions(garageUtil);
    }

    @Test
    @DisplayName("요청 Host와 Forwarded 헤더로 공개 게시글 및 이미지 URL을 바꿀 수 없다.")
    void test_untrusted_request_headers_do_not_change_public_urls() throws Exception {
        Post post = publishedPost();
        PostImage thumbnail = thumbnail(post);
        flushAndClear();

        String html = mockMvc.perform(get("/api/posts/og/{postId}", post.getId())
                .header(HttpHeaders.HOST, "attacker.example")
                .header("X-Forwarded-Host", "attacker.example")
                .header("X-Forwarded-Proto", "http"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(meta(html, "og:url")).isEqualTo(SITE_ORIGIN + "/posts/" + post.getId());
        assertThat(meta(html, "og:image")).isEqualTo(SITE_ORIGIN + "/api/post-images/" + thumbnail.getId());
        assertThat(html).doesNotContain("attacker.example");
    }

    @Test
    @DisplayName("OG 썸네일의 이미지 API는 인증 없이 접근할 수 있다.")
    void test_thumbnail_image_endpoint_is_public() throws Exception {
        Post post = publishedPost();
        PostImage thumbnail = thumbnail(post);
        flushAndClear();
        byte[] imageBytes = "stored-image-bytes".getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> stream = new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength((long) imageBytes.length).build(),
            new ByteArrayInputStream(imageBytes), Duration.ZERO
        );
        when(garageUtil.downloadObject(thumbnail.getObjectKey())).thenReturn(stream);

        mockMvc.perform(get("/api/post-images/{imageId}", thumbnail.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/webp"))
            .andExpect(content().bytes(imageBytes));
    }

    private Post publishedPost() {
        Category category = categoryRepository.save(new Category("OG 테스트", "og-test"));
        return postRepository.save(Post.publish("공개 게시글", "공개 요약", "<p>본문</p>", category));
    }

    private PostImage thumbnail(Post post) {
        PostImage image = new PostImage(PostImageType.THUMBNAIL,
            "posts/thumbnail/" + java.util.UUID.randomUUID() + ".webp", "image/webp", 960, 480);
        image.attachTo(post);
        return postImageRepository.save(image);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private String getHtml(Long postId) throws Exception {
        return mockMvc.perform(get("/api/posts/og/{postId}", postId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private void assertDefaultImage(String html) {
        assertThat(meta(html, "og:image")).isEqualTo(SITE_ORIGIN + "/og-default.png");
        assertThat(meta(html, "og:image:type")).isEqualTo("image/png");
        assertThat(meta(html, "og:image:width")).isEqualTo("1200");
        assertThat(meta(html, "og:image:height")).isEqualTo("630");
    }

    private void assertSameHeaders(MockHttpServletResponse getResponse, MockHttpServletResponse headResponse) {
        for (String header : List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.CACHE_CONTROL)) {
            assertThat(headResponse.getHeader(header)).isEqualTo(getResponse.getHeader(header));
        }
    }

    private String meta(String html, String name) {
        List<String> contents = new ArrayList<>();
        Matcher tags = META_TAG.matcher(html);
        while (tags.find()) {
            Map<String, String> attributes = new HashMap<>();
            Matcher attribute = ATTRIBUTE.matcher(tags.group(1));
            while (attribute.find()) {
                attributes.put(attribute.group(1), attribute.group(2));
            }
            if (name.equals(attributes.get("property")) || name.equals(attributes.get("name"))) {
                contents.add(attributes.get("content"));
            }
        }
        assertThat(contents).as("one %s meta tag", name).hasSize(1);
        return contents.getFirst();
    }
}
