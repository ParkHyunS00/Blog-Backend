package com.parkhyuns00.blog.domain.post.controller;

import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import com.parkhyuns00.blog.config.security.token.AdminKeyAuthenticationToken;
import com.parkhyuns00.blog.config.security.token.AdminOtpAuthenticationToken;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.util.GarageUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.jpa.open-in-view=false",
    "app.cors.allowed-origins=https://blog.parkhyuns00.com"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostImageAccessIntegrationTest {

    private static final byte[] IMAGE_BYTES = "stored-image-bytes".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private GarageUtil garageUtil;

    private final List<Long> imageIds = new ArrayList<>();
    private final List<Long> postIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();

    @AfterEach
    void cleanUpFixtures() {
        postImageRepository.deleteAllByIdInBatch(imageIds);
        postRepository.deleteAllByIdInBatch(postIds);
        categoryRepository.deleteAllByIdInBatch(categoryIds);
    }

    @ParameterizedTest
    @EnumSource(PostImageType.class)
    @DisplayName("공개 게시글의 썸네일과 본문 이미지는 익명 및 OTP 미완료 요청에도 제공한다.")
    void testPublishedImageIsPublic(PostImageType type) throws Exception {
        PostImage image = saveImage(type, savePost(PostStatus.PUBLISHED));
        stubDownload(image);

        mockMvc.perform(get("/api/post-images/{imageId}", image.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/webp"))
            .andExpect(content().bytes(IMAGE_BYTES))
            .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, IMAGE_BYTES.length));
        mockMvc.perform(get("/api/post-images/{imageId}", image.getId()).with(admin(AdminRole.PRE_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(content().bytes(IMAGE_BYTES));

        verify(garageUtil, times(2)).downloadObject(image.getObjectKey());
        verifyNoMoreInteractions(garageUtil);
    }

    @ParameterizedTest
    @CsvSource({"THUMBNAIL, DRAFT", "CONTENT, DRAFT", "THUMBNAIL, UNATTACHED", "CONTENT, UNATTACHED"})
    @DisplayName("임시저장 및 미첨부 이미지는 익명과 OTP 미완료 관리자에게 404를 반환한다.")
    void testPrivateImageIsHiddenWithoutFullAdmin(PostImageType type, String attachment) throws Exception {
        Post post = "DRAFT".equals(attachment) ? savePost(PostStatus.DRAFT) : null;
        PostImage image = saveImage(type, post);

        mockMvc.perform(get("/api/post-images/{imageId}", image.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("P_101"));
        mockMvc.perform(get("/api/post-images/{imageId}", image.getId()).with(admin(AdminRole.PRE_ADMIN)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("P_101"));
        mockMvc.perform(get("/api/post-images/{imageId}", image.getId()).with(user("visitor").roles("USER")))
            .andExpect(status().isNotFound());

        verifyNoInteractions(garageUtil);
    }

    @ParameterizedTest
    @CsvSource({
        "THUMBNAIL, PUBLISHED", "CONTENT, PUBLISHED",
        "THUMBNAIL, DRAFT", "CONTENT, DRAFT",
        "THUMBNAIL, UNATTACHED", "CONTENT, UNATTACHED"
    })
    @DisplayName("OTP 인증을 마친 관리자는 공개, 임시저장, 미첨부 이미지를 모두 조회한다.")
    void testAdminCanDownloadEveryExistingImage(PostImageType type, String attachment) throws Exception {
        Post post = "UNATTACHED".equals(attachment) ? null : savePost(PostStatus.valueOf(attachment));
        PostImage image = saveImage(type, post);
        stubDownload(image);

        mockMvc.perform(get("/api/post-images/{imageId}", image.getId()).with(admin(AdminRole.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/webp"))
            .andExpect(content().bytes(IMAGE_BYTES))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
            .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, IMAGE_BYTES.length));

        verify(garageUtil).downloadObject(image.getObjectKey());
        verifyNoMoreInteractions(garageUtil);
    }

    @Test
    @DisplayName("없는 이미지는 관리자와 익명 요청 모두 404를 반환하며 저장소 파일을 요청하지 않는다.")
    void testMissingImageReturns404ForAnonymousAndAdmin() throws Exception {
        mockMvc.perform(get("/api/post-images/{imageId}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("P_101"));
        mockMvc.perform(get("/api/post-images/{imageId}", Long.MAX_VALUE).with(admin(AdminRole.ADMIN)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("P_101"));

        verifyNoInteractions(garageUtil);
    }

    @ParameterizedTest
    @EnumSource(PostImageType.class)
    @DisplayName("같은 이미지 URL에서 게시글 발행 시 공개되고 연결 해제 후 다시 비공개가 된다.")
    void testPublicationAndDetachmentChangeAccessAtSameUrl(PostImageType type) throws Exception {
        Post post = savePost(PostStatus.DRAFT);
        PostImage image = saveImage(type, post);
        String imageUrl = "/api/post-images/" + image.getId();

        mockMvc.perform(get(imageUrl)).andExpect(status().isNotFound());
        verifyNoInteractions(garageUtil);

        post.publish();
        postRepository.saveAndFlush(post);
        stubDownload(image);

        mockMvc.perform(get(imageUrl))
            .andExpect(status().isOk())
            .andExpect(content().bytes(IMAGE_BYTES));

        image.detach();
        postImageRepository.saveAndFlush(image);

        mockMvc.perform(get(imageUrl)).andExpect(status().isNotFound());
        verify(garageUtil).downloadObject(image.getObjectKey());
        verifyNoMoreInteractions(garageUtil);
    }

    private Post savePost(PostStatus status) {
        String uniqueName = "image-access-" + UUID.randomUUID();
        Category category = categoryRepository.saveAndFlush(new Category(uniqueName, uniqueName));
        categoryIds.add(category.getId());
        Post post = status == PostStatus.PUBLISHED
            ? Post.publish("공개 게시글", "공개 요약", "<p>본문</p>", category)
            : Post.createDraft("임시 게시글", "임시 요약", "<p>본문</p>", category);
        post = postRepository.saveAndFlush(post);
        postIds.add(post.getId());
        return post;
    }

    private PostImage saveImage(PostImageType type, Post post) {
        PostImage image = new PostImage(type, "posts/" + UUID.randomUUID() + ".webp", "image/webp", 960, 480);
        if (post != null) {
            image.attachTo(post);
        }
        image = postImageRepository.saveAndFlush(image);
        imageIds.add(image.getId());
        return image;
    }

    private void stubDownload(PostImage image) {
        when(garageUtil.downloadObject(image.getObjectKey())).thenAnswer(invocation -> new ResponseInputStream<>(
            GetObjectResponse.builder().contentLength((long) IMAGE_BYTES.length).build(),
            new ByteArrayInputStream(IMAGE_BYTES), Duration.ZERO
        ));
    }

    private RequestPostProcessor admin(AdminRole role) {
        AdminPrincipal principal = new AdminPrincipal(role);
        return authentication(role == AdminRole.ADMIN
            ? new AdminOtpAuthenticationToken(principal, principal.getAuthorities())
            : new AdminKeyAuthenticationToken(principal, principal.getAuthorities()));
    }
}
