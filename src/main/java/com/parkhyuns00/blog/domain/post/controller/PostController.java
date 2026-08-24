package com.parkhyuns00.blog.domain.post.controller;

import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostRequest;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.*;
import com.parkhyuns00.blog.global.response.PageResponse;
import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

    @DeleteMapping("/api/admin/posts/{postId}")
    public ResponseEntity<StandardResponse<Void>> deletePublishedPost(@PathVariable Long postId) {
        postService.deletePublishedPost(postId);
        return StandardResponse.ok(null);
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

    @PostMapping("/api/admin/posts/draft/{postId}/publish")
    public ResponseEntity<StandardResponse<PostCreateDto>> publishDraft(
        @PathVariable Long postId, @Valid @RequestBody PostCreateRequest request
    ) {
        return StandardResponse.ok(postService.publishDraft(postId, request));
    }

    @GetMapping("/api/admin/posts/draft")
    public ResponseEntity<StandardResponse<PageResponse<PostDraftSummaryDto>>> getDraftPosts(
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.") int page
    ) {
        return StandardResponse.ok(PageResponse.from(postService.getDraftPosts(page)));
    }

    @GetMapping("/api/admin/posts/draft/{postId}")
    public ResponseEntity<StandardResponse<PostDraftDetailDto>> getDraftPost(@PathVariable Long postId) {
        return StandardResponse.ok(postService.getDraftPost(postId));
    }

    @DeleteMapping("/api/admin/posts/draft/{postId}")
    public ResponseEntity<StandardResponse<Void>> deleteDraft(@PathVariable Long postId) {
        postService.deleteDraft(postId);
        return StandardResponse.ok(null);
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
