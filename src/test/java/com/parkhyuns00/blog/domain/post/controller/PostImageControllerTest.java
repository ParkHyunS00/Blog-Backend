package com.parkhyuns00.blog.domain.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.service.PostImageService;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageUploadDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(PostImageController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostImageService postImageService;

    @Test
    @DisplayName("이미지 업로드가 성공하면 이미지 크기를 반환한다.")
    void test_upload_post_image_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.webp",
            "image/webp",
            "image-content".getBytes()
        );

        when(postImageService.upload(any(MultipartFile.class), eq(PostImageType.CONTENT)))
            .thenReturn(new PostImageUploadDto(
                1L,
                PostImageType.CONTENT,
                "posts/content/test.webp",
                "image/webp",
                1200,
                630)
            );

        mockMvc.perform(
            multipart("/api/admin/post-images")
                .file(file)
                .param("type", "CONTENT")
                .contentType(MediaType.MULTIPART_FORM_DATA)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.data.imageId").value(1))
            .andExpect(jsonPath("$.data.type").value("CONTENT"))
            .andExpect(jsonPath("$.data.objectKey").value("posts/content/test.webp"))
            .andExpect(jsonPath("$.data.mimeType").value("image/webp"))
            .andExpect(jsonPath("$.data.width").value(1200))
            .andExpect(jsonPath("$.data.height").value(630));

        verify(postImageService).upload(any(MultipartFile.class), eq(PostImageType.CONTENT));
    }
}
