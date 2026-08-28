package com.parkhyuns00.blog.domain.tag.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.parkhyuns00.blog.domain.tag.service.TagService;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

@WebMvcTest(TagController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    @DisplayName("태그 목록 조회가 성공하면 태그 정보를 반환한다.")
    void test_get_tags_success() throws Exception {
        when(tagService.getTags()).thenReturn(List.of(
            new TagDto(1L, "Java", "java"),
            new TagDto(2L, "Spring", "spring")
        ));

        mockMvc.perform(get("/api/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data[0].tagId").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Java"))
            .andExpect(jsonPath("$.data[0].slug").value("java"))
            .andExpect(jsonPath("$.data[1].tagId").value(2))
            .andExpect(jsonPath("$.data[1].slug").value("spring"))
            .andDo(print());

        verify(tagService).getTags();
    }

    @Test
    @DisplayName("태그가 없으면 빈 목록을 반환한다.")
    void test_get_tags_empty() throws Exception {
        when(tagService.getTags()).thenReturn(List.of());

        mockMvc.perform(get("/api/tags"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").isEmpty())
            .andDo(print());

        verify(tagService).getTags();
    }
}
