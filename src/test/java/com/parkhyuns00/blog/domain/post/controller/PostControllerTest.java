package com.parkhyuns00.blog.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.PostCreateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Test
    @DisplayName("게시글 생성 요청이 성공하면 201 응답과 생성 결과를 반환한다.")
    void test_create_post_success() throws Exception {
        when(postService.create(any(PostCreateRequest.class))).thenReturn(new PostCreateDto(1L, PostStatus.PUBLISHED));

        mockMvc.perform(post("/api/admin/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "content",
                    "status": "PUBLISHED",
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": 1,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andDo(print());

        ArgumentCaptor<PostCreateRequest> captor = ArgumentCaptor.forClass(PostCreateRequest.class);
        verify(postService).create(captor.capture());

        PostCreateRequest request = captor.getValue();
        assertThat(request.title()).isEqualTo("title");
        assertThat(request.summary()).isEqualTo("summary");
        assertThat(request.content()).isEqualTo("content");
        assertThat(request.status()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(request.categoryName()).isEqualTo("Spring");
        assertThat(request.tagNames()).containsExactly("Java", "Spring");
        assertThat(request.thumbnailImageId()).isEqualTo(1L);
        assertThat(request.contentImageIds()).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("제목이 공백이면 400 응답을 반환한다.")
    void test_create_post_fail_when_title_blank() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "   ",
                    "summary": "summary",
                    "content": "content",
                    "status": "PUBLISHED",
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": 1,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("요약이 공백이면 400 응답을 반환한다.")
    void test_create_post_fail_when_summary_blank() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": " ",
                    "content": "content",
                    "status": "PUBLISHED",
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": 1,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("본문이 공백이면 400 응답을 반환한다.")
    void test_create_post_fail_when_content_blank() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "     ",
                    "status": "PUBLISHED",
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": 1,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("게시글 상태가 null 이면 400 응답을 반환한다.")
    void test_create_post_fail_when_status_null() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "summary",
                    "status": null,
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": 1,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 이름이 공백이면 400 응답을 반환한다.")
    void test_create_post_fail_when_category_blank() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "summary",
                    "status": "PUBLISHED",
                    "categoryName": "  ",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": 1,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("썸네일 이미지가 null 이면 400 응답을 반환한다.")
    void test_create_post_fail_when_thumbnail_image_null() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "summary",
                    "status": "PUBLISHED",
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": null,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("태그 목록과 본문 이미지 목록이 null 이어도 게시글 생성은 성공한다.")
    void test_create_post_success_when_optional_list_null() throws Exception {
        when(postService.create(any(PostCreateRequest.class))).thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "summary",
                    "status": "PUBLISHED",
                    "categoryName": "Spring",
                    "tagNames": null,
                    "thumbnailImageId": 1,
                    "contentImageIds": null
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andDo(print());

        verify(postService).create(any(PostCreateRequest.class));
    }
}
