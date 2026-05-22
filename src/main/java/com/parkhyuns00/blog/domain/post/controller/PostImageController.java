package com.parkhyuns00.blog.domain.post.controller;

import com.parkhyuns00.blog.domain.post.controller.dto.ImageUploadResponse;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.service.PostImageService;
import com.parkhyuns00.blog.domain.post.service.dto.PostImageDownloadDto;
import com.parkhyuns00.blog.global.response.StandardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PostImageController {

    private final PostImageService postImageService;

    @PostMapping(value = "/api/admin/post-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StandardResponse<ImageUploadResponse>> upload(
        @RequestPart("file")MultipartFile file,
        @RequestParam("type")PostImageType type
    ) {
        return StandardResponse.created(ImageUploadResponse.from(postImageService.upload(file, type)));
    }

    @GetMapping("/api/post-images/{imageId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long imageId) {
        PostImageDownloadDto downloadDto = postImageService.download(imageId);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(downloadDto.mimeType()))
            .contentLength(downloadDto.contentLength())
            .body(new InputStreamResource(downloadDto.inputStream()));
    }

    @DeleteMapping("/api/admin/post-images/{imageId}")
    public ResponseEntity<StandardResponse<Void>> delete(@PathVariable Long imageId) {
        postImageService.delete(imageId);

        return StandardResponse.ok(null);
    }
}
