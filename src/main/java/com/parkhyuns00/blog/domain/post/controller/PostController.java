package com.parkhyuns00.blog.domain.post.controller;

import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostRequest;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.PostCreateDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import com.parkhyuns00.blog.global.response.PageResponse;
import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/api/admin/posts")
    public ResponseEntity<StandardResponse<PostCreateDto>> create(@Valid @RequestBody PostCreateRequest request) {
        return StandardResponse.created(postService.create(request));
    }

    @GetMapping("/api/posts")
    public ResponseEntity<StandardResponse<PageResponse<PostSummaryDto>>> getPublishedPosts(
        @ModelAttribute PostRequest request, @PageableDefault(size = 5) Pageable pageable
    ) {
        Page<PostSummaryDto> posts = postService.getPublishedPosts(request.toCondition(), pageable);

        return StandardResponse.ok(PageResponse.from(posts));
    }
}
