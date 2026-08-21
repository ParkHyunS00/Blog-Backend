package com.parkhyuns00.blog.domain.post.controller;

import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostRequest;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.PostCreateDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import com.parkhyuns00.blog.global.response.PageResponse;
import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @PostMapping("/api/admin/posts/draft")
    public ResponseEntity<StandardResponse<PostCreateDto>> createDraft(@Valid @RequestBody PostDraftCreateRequest request) {
        return StandardResponse.created(postService.createDraft(request));
    }

    @PutMapping("/api/admin/posts/draft/{postId}")
    public ResponseEntity<StandardResponse<PostCreateDto>> updateDraft(
        @PathVariable Long postId, @Valid @RequestBody PostDraftUpdateRequest request
    ) {
        return StandardResponse.ok(postService.updateDraft(postId, request));
    }

    @GetMapping("/api/posts")
    public ResponseEntity<StandardResponse<PageResponse<PostSummaryDto>>> getPublishedPosts(
        @Valid @ModelAttribute PostRequest request
    ) {
        Page<PostSummaryDto> posts = postService.getPublishedPosts(request.toCondition(), request.toPageable());

        return StandardResponse.ok(PageResponse.from(posts));
    }

    @GetMapping("/api/posts/{postId}")
    public ResponseEntity<StandardResponse<PostDetailDto>> getPublishedPost(@PathVariable Long postId) {
        return StandardResponse.ok(postService.getPublishedPost(postId));
    }
}
