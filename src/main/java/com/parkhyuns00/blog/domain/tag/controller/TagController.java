package com.parkhyuns00.blog.domain.tag.controller;

import com.parkhyuns00.blog.domain.tag.service.TagService;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.parkhyuns00.blog.global.response.StandardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/tags")
    public ResponseEntity<StandardResponse<List<TagDto>>> getTags() {
        return StandardResponse.ok(tagService.getTags());
    }
}
