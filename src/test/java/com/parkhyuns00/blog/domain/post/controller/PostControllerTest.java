package com.parkhyuns00.blog.domain.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.*;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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
    @DisplayName("썸네일 이미지가 없어도 게시글 생성 요청에 성공한다.")
    void test_create_post_fail_when_thumbnail_image_null() throws Exception {
        when(postService.create(any(PostCreateRequest.class))).thenReturn(new PostCreateDto(1L, PostStatus.PUBLISHED));

        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "summary",
                    "categoryName": "Spring",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": null,
                    "contentImageIds": [2, 3]
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status")
            .value("PUBLISHED"));

        ArgumentCaptor<PostCreateRequest> captor = ArgumentCaptor.forClass(PostCreateRequest.class);

        verify(postService).create(captor.capture());

        assertThat(captor.getValue().thumbnailImageId()).isNull();
    }

    @Test
    @DisplayName("태그 목록과 본문 이미지 목록이 null 이어도 게시글 생성은 성공한다.")
    void test_create_post_success_when_optional_list_null() throws Exception {
        when(postService.create(any(PostCreateRequest.class))).thenReturn(new PostCreateDto(1L, PostStatus.PUBLISHED));

        mockMvc.perform(post("/api/admin/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {
                    "title": "title",
                    "summary": "summary",
                    "content": "summary",
                    "categoryName": "Spring",
                    "tagNames": null,
                    "thumbnailImageId": 1,
                    "contentImageIds": null
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andDo(print());

        verify(postService).create(any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("공개 게시글 목록 조회가 성공하면 페이지 응답을 반환한다.")
    void test_get_published_posts_success() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);

        PostSummaryDto summary = new PostSummaryDto(
            1L,
            "title",
            "summary",
            10L,
            "Backend",
            "backend",
            List.of(
                new TagDto(1L, "Java", "java"),
                new TagDto(2L, "Spring", "spring")
            ),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(postService.getPublishedPosts(any(), any()))
            .thenReturn(new PageImpl<>(
                List.of(summary),
                pageable,
                1
            ));

        mockMvc.perform(get("/api/posts")
                .param("tags", "java", "spring")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.content[0].postId").value(1))
            .andExpect(jsonPath("$.data.content[0].title").value("title"))
            .andExpect(jsonPath("$.data.content[0].categorySlug").value("backend"))
            .andExpect(jsonPath("$.data.content[0].tags[0].slug").value("java"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(5))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.totalPages").value(1))
            .andExpect(jsonPath("$.data.hasNext").value(false))
            .andExpect(jsonPath("$.data.hasPrevious").value(false))
            .andDo(print());

        ArgumentCaptor<PostSearchCondition> conditionCaptor =
            ArgumentCaptor.forClass(PostSearchCondition.class);

        verify(postService).getPublishedPosts(
            conditionCaptor.capture(),
            any(Pageable.class)
        );

        PostSearchCondition condition = conditionCaptor.getValue();

        assertThat(condition.categorySlug()).isNull();
        assertThat(condition.tagSlugs()).containsExactly("java", "spring");
        assertThat(condition.keyword()).isNull();
    }

    @Test
    @DisplayName("카테고리 조건으로 공개 게시글 목록을 조회한다.")
    void test_get_published_posts_by_category_success() throws Exception {
        when(postService.getPublishedPosts(any(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/api/posts")
                .param("category", "backend")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk());

        ArgumentCaptor<PostSearchCondition> conditionCaptor =
            ArgumentCaptor.forClass(PostSearchCondition.class);

        verify(postService).getPublishedPosts(conditionCaptor.capture(), any(Pageable.class));

        PostSearchCondition condition = conditionCaptor.getValue();

        assertThat(condition.categorySlug()).isEqualTo("backend");
        assertThat(condition.tagSlugs()).isEmpty();
        assertThat(condition.keyword()).isNull();
    }

    @Test
    @DisplayName("게시글 목록 조회 시 페이지 번호가 음수이면 400 을 반환한다.")
    void test_get_published_posts_fail_when_page_negative() throws Exception {
        mockMvc.perform(get("/api/posts")
                .param("page", "-1")
                .param("size", "5"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error.code").value("COMMON_001"));

        verify(postService, never()).getPublishedPosts(any(), any());
    }

    @Test
    @DisplayName("게시글 목록 조회 시 페이지 크기가 최대값을 초과하면 400 을 반환한다.")
    void test_get_published_posts_fail_when_size_exceeded() throws Exception {
        mockMvc.perform(get("/api/posts")
                .param("page", "0")
                .param("size", "6"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error.code").value("COMMON_001"));

        verify(postService, never()).getPublishedPosts(any(), any());
    }

    @Test
    @DisplayName("게시글 목록 조회 시 페이지 크기가 최대값이면 게시글 목록을 조회한다.")
    void test_get_published_posts_success_when_size_maximum() throws Exception {
        when(postService.getPublishedPosts(any(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/api/posts")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk());

        verify(postService).getPublishedPosts(any(), any());
    }

    @Test
    @DisplayName("게시글 목록 조회 시 페이지 크기가 0이면 400 을 반환한다.")
    void test_get_published_posts_fail_when_size_zero() throws Exception {
        mockMvc.perform(get("/api/posts")
                .param("page", "0")
                .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error.code").value("COMMON_001"));

        verify(postService, never())
            .getPublishedPosts(any(), any());
    }

    @Test
    @DisplayName("게시글 목록 조회 시 페이지 요청값이 없으면 기본값을 사용한다.")
    void test_get_published_posts_with_default_page_success() throws Exception {
        when(postService.getPublishedPosts(any(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(postService).getPublishedPosts(
            any(PostSearchCondition.class),
            pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("공개 게시글 상세 조회가 성공하면 게시글 정보를 반환한다.")
    void test_get_published_post_success() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        PostDetailDto detail = new PostDetailDto(
            1L,
            "title",
            "summary",
            "content",
            10L,
            "Backend",
            "backend",
            List.of(
                new TagDto(1L, "Java", "java"),
                new TagDto(2L, "Spring", "spring")
            ),
            List.of(11L, 12L),
            now,
            now
        );

        when(postService.getPublishedPost(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/posts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.title").value("title"))
            .andExpect(jsonPath("$.data.summary").value("summary"))
            .andExpect(jsonPath("$.data.content").value("content"))
            .andExpect(jsonPath("$.data.thumbnailImageId").value(10))
            .andExpect(jsonPath("$.data.categoryName").value("Backend"))
            .andExpect(jsonPath("$.data.categorySlug").value("backend"))
            .andExpect(jsonPath("$.data.tags[0].slug").value("java"))
            .andExpect(jsonPath("$.data.tags[1].slug").value("spring"))
            .andExpect(jsonPath("$.data.contentImageIds[0]").value(11))
            .andExpect(jsonPath("$.data.contentImageIds[1]").value(12))
            .andDo(print());

        verify(postService).getPublishedPost(1L);
    }

    @Test
    @DisplayName("공개 게시글을 찾을 수 없으면 404 를 반환한다.")
    void test_get_published_post_fail_when_not_found() throws Exception {
        when(postService.getPublishedPost(999L)).thenThrow(new PostException(PostExceptionCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/posts/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error.code").value("P_001"))
            .andExpect(jsonPath("$.error.message").value("게시글을 찾을 수 없습니다."))
            .andDo(print());

        verify(postService).getPublishedPost(999L);
    }

    @Test
    @DisplayName("검색어로 공개 게시글 목록을 조회한다.")
    void test_get_published_posts_by_keyword_success() throws Exception {
        when(postService.getPublishedPosts(any(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 5)));

        mockMvc.perform(get("/api/posts")
                .param("keyword", "  스프링 보안  ")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk());

        ArgumentCaptor<PostSearchCondition> conditionCaptor = ArgumentCaptor.forClass(PostSearchCondition.class);

        verify(postService).getPublishedPosts(conditionCaptor.capture(), any(Pageable.class));

        PostSearchCondition condition = conditionCaptor.getValue();

        assertThat(condition.categorySlug()).isNull();
        assertThat(condition.tagSlugs()).isEmpty();
        assertThat(condition.keyword()).isEqualTo("스프링 보안");
    }

    @Test
    @DisplayName("게시글 임시저장 요청이 성공하면 201 응답을 반환한다.")
    void test_create_draft_success() throws Exception {
        when(postService.createDraft(any(PostDraftCreateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(
                post("/api/admin/posts/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                      {
                          "title": "draft title",
                          "summary": "draft summary",
                          "content": "<p>draft content</p>",
                          "categoryName": "Spring",
                          "tagNames": ["Java", "Spring"],
                          "thumbnailImageId": 1,
                          "contentImageIds": [2, 3]
                      }
                      """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andDo(print());

        ArgumentCaptor<PostDraftCreateRequest> captor = ArgumentCaptor.forClass(PostDraftCreateRequest.class);

        verify(postService).createDraft(captor.capture());

        PostDraftCreateRequest request = captor.getValue();

        assertThat(request.title()).isEqualTo("draft title");
        assertThat(request.summary()).isEqualTo("draft summary");
        assertThat(request.content()).isEqualTo("<p>draft content</p>");
        assertThat(request.categoryName()).isEqualTo("Spring");
        assertThat(request.tagNames()).containsExactly("Java", "Spring");
        assertThat(request.thumbnailImageId()).isEqualTo(1L);
        assertThat(request.contentImageIds()).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("작성 내용이 없어도 게시글 임시저장 요청에 성공한다.")
    void test_create_draft_success_when_content_empty() throws Exception {
        when(postService.createDraft(any(PostDraftCreateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(
                post("/api/admin/posts/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        ArgumentCaptor<PostDraftCreateRequest> captor = ArgumentCaptor.forClass(PostDraftCreateRequest.class);

        verify(postService).createDraft(captor.capture());

        PostDraftCreateRequest request = captor.getValue();

        assertThat(request.title()).isNull();
        assertThat(request.summary()).isNull();
        assertThat(request.content()).isNull();
        assertThat(request.categoryName()).isNull();
    }

    @Test
    @DisplayName("임시저장 제목이 200자를 초과하면 400 응답을 반환한다.")
    void test_create_draft_fail_when_title_too_long() throws Exception {
        String title = "a".repeat(201);

        mockMvc.perform(
            post("/api/admin/posts/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {
                      "title": "%s"
                  }
                  """.formatted(title))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        verify(postService, never()).createDraft(any(PostDraftCreateRequest.class));
    }

    @Test
    @DisplayName("게시글 임시저장 수정 요청이 성공하면 수정 결과를 반환한다.")
    void test_update_draft_success() throws Exception {
        when(postService.updateDraft(eq(1L), any(PostDraftUpdateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(
            put("/api/admin/posts/draft/{postId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {
                      "title": "updated title",
                      "summary": "updated summary",
                      "content": "<p>updated content</p>",
                      "categoryName": "Spring",
                      "tagNames": ["Java", "Spring"],
                      "thumbnailImageId": 1,
                      "contentImageIds": [2, 3]
                  }
                  """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andDo(print());

        ArgumentCaptor<PostDraftUpdateRequest> captor = ArgumentCaptor.forClass(PostDraftUpdateRequest.class);

        verify(postService).updateDraft(eq(1L), captor.capture());

        PostDraftUpdateRequest request = captor.getValue();

        assertThat(request.title()).isEqualTo("updated title");
        assertThat(request.summary()).isEqualTo("updated summary");
        assertThat(request.content()).isEqualTo("<p>updated content</p>");
        assertThat(request.categoryName()).isEqualTo("Spring");
        assertThat(request.tagNames()).containsExactly("Java", "Spring");
        assertThat(request.thumbnailImageId()).isEqualTo(1L);
        assertThat(request.contentImageIds()).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("작성 내용을 비운 상태로 게시글 임시저장을 수정할 수 있다.")
    void test_update_draft_success_when_content_empty() throws Exception {
        when(postService.updateDraft(eq(1L), any(PostDraftUpdateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.DRAFT));

        mockMvc.perform(
            put("/api/admin/posts/draft/{postId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(postService).updateDraft(eq(1L), any(PostDraftUpdateRequest.class));
    }

    @Test
    @DisplayName("임시저장 수정 제목이 200자를 초과하면 400 응답을 반환한다.")
    void test_update_draft_fail_when_title_too_long() throws Exception {
        String title = "a".repeat(201);

        mockMvc.perform(
            put("/api/admin/posts/draft/{postId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {
                      "title": "%s"
                  }
                  """.formatted(title)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        verify(postService, never()).updateDraft(anyLong(), any(PostDraftUpdateRequest.class));
    }

    @Test
    @DisplayName("게시글 임시저장 목록 조회가 성공하면 페이지 응답을 반환한다.")
    void test_get_draft_posts_success() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.now();

        PostDraftSummaryDto summary = new PostDraftSummaryDto(
            1L,
            "draft title",
            new CategoryDto(
                10L,
                "Spring",
                "spring"
            ),
            List.of(
                new TagDto(
                    20L,
                    "Java",
                    "java"
                )
            ),
            updatedAt
        );

        when(postService.getDraftPosts(0)).thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1));

        mockMvc.perform(
                get("/api/admin/posts/draft").param("page", "0")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.content[0].postId").value(1))
            .andExpect(jsonPath("$.data.content[0].title").value("draft title"))
            .andExpect(jsonPath("$.data.content[0].category.categoryId").value(10))
            .andExpect(jsonPath("$.data.content[0].category.name").value("Spring"))
            .andExpect(jsonPath("$.data.content[0].category.slug").value("spring"))
            .andExpect(jsonPath("$.data.content[0].tags[0].tagId").value(20))
            .andExpect(jsonPath("$.data.content[0].tags[0].name").value("Java"))
            .andExpect(jsonPath("$.data.content[0].tags[0].slug").value("java"))
            .andExpect(jsonPath("$.data.content[0].updatedAt").exists())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.totalPages").value(1))
            .andExpect(jsonPath("$.data.hasNext").value(false))
            .andExpect(jsonPath("$.data.hasPrevious").value(false));

        verify(postService).getDraftPosts(0);
    }

    @Test
    @DisplayName("임시저장 목록 조회 시 페이지 번호가 없으면 첫 페이지를 조회한다.")
    void test_get_draft_posts_with_default_page_success() throws Exception {
        when(postService.getDraftPosts(0))
            .thenReturn(Page.empty(PageRequest.of(0, 10)));

        mockMvc.perform(get("/api/admin/posts/draft"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10));

        verify(postService).getDraftPosts(0);
    }

    @Test
    @DisplayName("임시저장 목록 조회 시 페이지 번호가 음수이면 400을 반환한다.")
    void test_get_draft_posts_fail_when_page_negative() throws Exception {
        mockMvc.perform(
            get("/api/admin/posts/draft")
                .param("page", "-1")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error.code").value("COMMON_001"));

        verify(postService, never()).getDraftPosts(anyInt());
    }

    @Test
    @DisplayName("게시글 임시저장 상세 조회가 성공하면 에디터 복원 정보를 반환한다.")
    void test_get_draft_post_success() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        PostDraftDetailDto detail = new PostDraftDetailDto(
            1L,
            "draft title",
            "draft summary",
            "<p>draft content</p>",
            new CategoryDto(10L, "Spring", "spring"),
            List.of(
                new TagDto(20L, "Java", "java"),
                new TagDto(21L, "Spring", "spring")
            ),
            30L,
            List.of(31L, 32L),
            now,
            now
        );

        when(postService.getDraftPost(1L)).thenReturn(detail);

        mockMvc.perform(
            get(
                "/api/admin/posts/draft/{postId}",
                1L
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.title").value("draft title"))
            .andExpect(jsonPath("$.data.summary").value("draft summary"))
            .andExpect(jsonPath("$.data.content").value("<p>draft content</p>"))
            .andExpect(jsonPath("$.data.category.categoryId").value(10))
            .andExpect(jsonPath("$.data.category.name").value("Spring"))
            .andExpect(jsonPath("$.data.category.slug").value("spring"))
            .andExpect(jsonPath("$.data.tags[0].tagId").value(20))
            .andExpect(jsonPath("$.data.tags[0].slug").value("java"))
            .andExpect(jsonPath("$.data.tags[1].tagId").value(21))
            .andExpect(jsonPath("$.data.thumbnailImageId").value(30))
            .andExpect(jsonPath("$.data.contentImageIds[0]").value(31))
            .andExpect(jsonPath("$.data.contentImageIds[1]").value(32))
            .andExpect(jsonPath("$.data.createdAt").exists())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andDo(print());

        verify(postService).getDraftPost(1L);
    }

    @Test
    @DisplayName("작성 내용과 연관 정보가 없는 게시글 임시저장도 상세 조회한다.")
    void test_get_empty_draft_post_success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        PostDraftDetailDto detail = new PostDraftDetailDto(
            1L,
            "",
            "",
            "",
            null,
            List.of(),
            null,
            List.of(),
            now,
            now
        );

        when(postService.getDraftPost(1L)).thenReturn(detail);

        mockMvc.perform(
            get(
                "/api/admin/posts/draft/{postId}",
                1L
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value(""))
            .andExpect(jsonPath("$.data.summary").value(""))
            .andExpect(jsonPath("$.data.content").value(""))
            .andExpect(jsonPath("$.data.category").doesNotExist())
            .andExpect(jsonPath("$.data.tags").isEmpty())
            .andExpect(jsonPath("$.data.thumbnailImageId").doesNotExist())
            .andExpect(jsonPath("$.data.contentImageIds").isEmpty());

        verify(postService).getDraftPost(1L);
    }

    @Test
    @DisplayName("게시글 임시저장을 찾을 수 없으면 404를 반환한다.")
    void test_get_draft_post_fail_when_not_found() throws Exception {
        when(postService.getDraftPost(999L)).thenThrow(new PostException(PostExceptionCode.POST_NOT_FOUND));

        mockMvc.perform(
            get(
                "/api/admin/posts/draft/{postId}",
                999L
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error.code").value("P_001"))
            .andExpect(jsonPath("$.error.message").value("게시글을 찾을 수 없습니다."));

        verify(postService).getDraftPost(999L);
    }

    @Test
    @DisplayName("게시글 임시저장 삭제 요청이 성공하면 200을 반환한다.")
    void test_delete_draft_success() throws Exception {
        doNothing()
            .when(postService)
            .deleteDraft(1L);

        mockMvc.perform(
            delete(
                "/api/admin/posts/draft/{postId}",
                1L
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(postService).deleteDraft(1L);
    }

    @Test
    @DisplayName("삭제할 게시글 임시저장을 찾을 수 없으면 404를 반환한다.")
    void test_delete_draft_fail_when_not_found() throws Exception {
        doThrow(new PostException(PostExceptionCode.POST_NOT_FOUND))
            .when(postService)
            .deleteDraft(999L);

        mockMvc.perform(
            delete(
                "/api/admin/posts/draft/{postId}",
                999L
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error.code").value("P_001"))
            .andExpect(jsonPath("$.error.message").value("게시글을 찾을 수 없습니다."));

        verify(postService).deleteDraft(999L);
    }

    @Test
    @DisplayName("게시글 임시저장 발행 요청이 성공하면 최종 내용을 반영한 결과를 반환한다.")
    void test_publish_draft_success() throws Exception {
        when(postService.publishDraft(eq(1L), any(PostCreateRequest.class)))
            .thenReturn(new PostCreateDto(1L, PostStatus.PUBLISHED));

        mockMvc.perform(
            post("/api/admin/posts/draft/{postId}/publish", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    "title": "최종 제목",
                    "summary": "최종 요약",
                    "content": "<p>최종 본문</p>",
                    "categoryName": "Backend",
                    "tagNames": ["Java", "Spring"],
                    "thumbnailImageId": null,
                    "contentImageIds": []
                    }
                    """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.postId").value(1))
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
            .andDo(print());

        ArgumentCaptor<PostCreateRequest> requestCaptor = ArgumentCaptor.forClass(PostCreateRequest.class);

        verify(postService).publishDraft(eq(1L), requestCaptor.capture());

        PostCreateRequest request = requestCaptor.getValue();

        assertThat(request.title()).isEqualTo("최종 제목");
        assertThat(request.summary()).isEqualTo("최종 요약");
        assertThat(request.content()).isEqualTo("<p>최종 본문</p>");
        assertThat(request.categoryName()).isEqualTo("Backend");
        assertThat(request.tagNames()).containsExactly("Java", "Spring");
        assertThat(request.thumbnailImageId()).isNull();
        assertThat(request.contentImageIds()).isEmpty();
    }

    @Test
    @DisplayName("게시글 임시저장 발행 요청의 제목이 공백이면 400 응답을 반환한다.")
    void test_publish_draft_fail_when_title_blank() throws Exception {

        mockMvc.perform(
            post("/api/admin/posts/draft/{postId}/publish", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    "title": " ",
                    "summary": "최종 요약",
                    "content": "<p>최종 본문</p>",
                    "categoryName": "Backend",
                    "tagNames": [],
                    "thumbnailImageId": null,
                    "contentImageIds": []
                    }
                  """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andDo(print());

        verify(postService, never()).publishDraft(anyLong(), any(PostCreateRequest.class));
    }

    @Test
    @DisplayName("발행할 게시글 임시저장을 찾을 수 없으면 404 응답을 반환한다.")
    void test_publish_draft_fail_when_not_found() throws Exception {
        when(postService.publishDraft(eq(999L), any(PostCreateRequest.class)))
            .thenThrow(new PostException(PostExceptionCode.POST_NOT_FOUND));

        mockMvc.perform(
            post("/api/admin/posts/draft/{postId}/publish", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                    "title": "최종 제목",
                    "summary": "최종 요약",
                    "content": "<p>최종 본문</p>",
                    "categoryName": "Backend",
                    "tagNames": [],
                    "thumbnailImageId": null,
                    "contentImageIds": []
                    }
                    """)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error.code").value("P_001"))
            .andDo(print());

        verify(postService).publishDraft(eq(999L), any(PostCreateRequest.class));
    }
}
