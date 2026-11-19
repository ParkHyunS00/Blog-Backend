package com.parkhyuns00.blog.domain.post.controller;

import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftUpdateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostRequest;
import com.parkhyuns00.blog.domain.post.service.PostService;
import com.parkhyuns00.blog.domain.post.service.dto.*;
import com.parkhyuns00.blog.global.exception.BusinessException;
import com.parkhyuns00.blog.global.exception.CommonExceptionCode;
import com.parkhyuns00.blog.global.response.PageResponse;
import com.parkhyuns00.blog.global.response.StandardResponse;
import com.parkhyuns00.blog.global.web.cookie.VisitorCookieManager;
import com.parkhyuns00.blog.util.OpenGraphUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    private static final String POST_VIEWER_COOKIE_NAME = "POST_VIEWER_ID";

    private final PostService postService;
    private final VisitorCookieManager visitorCookieManager;
    private final OpenGraphUtil openGraphUtil;

    @PostMapping("/api/admin/posts")
    public ResponseEntity<StandardResponse<PostCreateDto>> create(@Valid @RequestBody PostCreateRequest request) {
        return StandardResponse.created(postService.create(request));
    }

    @DeleteMapping("/api/admin/posts/{postId}")
    public ResponseEntity<StandardResponse<Void>> deletePublishedPost(@PathVariable Long postId) {
        postService.deletePublishedPost(postId);
        return StandardResponse.ok(null);
    }

    @PutMapping("/api/admin/posts/{postId}")
    public ResponseEntity<StandardResponse<PostCreateDto>> updatePublishedPost(
        @PathVariable Long postId, @Valid @RequestBody PostCreateRequest request
    ) {
        return StandardResponse.ok(postService.updatePublishedPost(postId, request));
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
    public ResponseEntity<StandardResponse<PostDetailDto>> getPublishedPost(
        @PathVariable Long postId,
        @CookieValue(name = POST_VIEWER_COOKIE_NAME, required = false) String visitorCookie,
        HttpServletResponse response
    ) {
        UUID visitorId = visitorCookieManager.resolve(POST_VIEWER_COOKIE_NAME, visitorCookie, response);

        return StandardResponse.ok(postService.getPublishedPost(postId, visitorId));
    }

    @GetMapping("/api/posts/popular")
    public ResponseEntity<StandardResponse<List<PostPopularDto>>> getPopularPosts() {
        return StandardResponse.ok(postService.getPopularPosts());
    }

    @GetMapping("/api/posts/og/{postId}")
    public ResponseEntity<String> getPublishedPostOpenGraph(@PathVariable String postId) {
        long id;
        try {
            id = Long.parseLong(postId);
            if (id <= 0 || !postId.matches("[0-9]+")) {
                throw new NumberFormatException("게시글 ID는 양의 정수여야 합니다.");
            }
        } catch (NumberFormatException exception) {
            return ResponseEntity.status(CommonExceptionCode.INVALID_REQUEST.getHttpStatus())
                .cacheControl(CacheControl.noStore()).contentLength(0).build();
        }

        // HTML-only crawlers may not accept JSON errors. Keep exception statuses
        // and bodyless errors local to this endpoint, preserving the JSON APIs.
        try {
            String html = openGraphUtil.render(postService.getPublishedPostOpenGraph(id));
            return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(html);
        } catch (BusinessException exception) {
            log.warn("[BusinessException] code={}, message={}, detail={}",
                exception.getExceptionCode().getCode(), exception.getExceptionCode().getMessage(), exception.getDetail());
            return ResponseEntity.status(exception.getExceptionCode().getHttpStatus())
                .cacheControl(CacheControl.noStore()).contentLength(0).build();
        } catch (Exception exception) {
            log.error("Unexpected Open Graph exception", exception);
            return ResponseEntity.status(CommonExceptionCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .cacheControl(CacheControl.noStore()).contentLength(0).build();
        }
    }
}
