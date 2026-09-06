package com.parkhyuns00.blog.domain.category.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.parkhyuns00.blog.domain.category.service.CategoryService;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryLatestPostDto;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryWithLatestPostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리별 최신 게시글 조회가 성공하면 카테고리와 게시글 정보를 반환한다.")
    void test_get_categories_with_latest_post_success() throws Exception {
        when(categoryService.getCategoriesWithLatestPost())
            .thenReturn(List.of(
                new CategoryWithLatestPostDto(
                    1L,
                    "Backend",
                    "backend",
                    3L,
                    new CategoryLatestPostDto(
                        10L,
                        "게시글 제목",
                        "게시글 요약",
                        LocalDateTime.of(2026, 9, 1, 12, 0)
                    )
                )
            ));

        mockMvc.perform(get("/api/categories/latest-posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].categoryId").value(1))
            .andExpect(jsonPath("$.data[0].categoryName").value("Backend"))
            .andExpect(jsonPath("$.data[0].categorySlug").value("backend"))
            .andExpect(jsonPath("$.data[0].postCount").value(3))
            .andExpect(jsonPath("$.data[0].latestPost.postId").value(10))
            .andExpect(jsonPath("$.data[0].latestPost.title").value("게시글 제목"))
            .andExpect(jsonPath("$.data[0].latestPost.summary").value("게시글 요약"))
            .andExpect(jsonPath("$.data[0].latestPost.createdAt").value("2026-09-01T12:00:00"));

        verify(categoryService).getCategoriesWithLatestPost();
    }

    @Test
    @DisplayName("조회할 카테고리가 없으면 200 응답과 빈 배열을 반환한다.")
    void test_get_categories_with_latest_post_success_when_empty() throws Exception {
        when(categoryService.getCategoriesWithLatestPost())
            .thenReturn(List.of());

        mockMvc.perform(get("/api/categories/latest-posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());

        verify(categoryService).getCategoriesWithLatestPost();
    }
}
