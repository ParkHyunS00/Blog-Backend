package com.parkhyuns00.blog.domain.visitor.controller;

import com.parkhyuns00.blog.domain.visitor.service.VisitorService;
import com.parkhyuns00.blog.domain.visitor.service.dto.VisitorStatsDto;
import com.parkhyuns00.blog.global.response.StandardResponse;
import com.parkhyuns00.blog.global.web.cookie.VisitorCookieManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VisitorController {

    private static final String VISITOR_COOKIE_NAME = "VISITOR_ID";

    private final VisitorService visitorService;
    private final VisitorCookieManager visitorCookieManager;

    @PostMapping("/api/visitors")
    public ResponseEntity<StandardResponse<Void>> recordVisit(
        @CookieValue(name = VISITOR_COOKIE_NAME, required = false) String visitorCookie, HttpServletResponse response
    ) {
        UUID visitorId = visitorCookieManager.resolve(VISITOR_COOKIE_NAME, visitorCookie, response);

        visitorService.recordVisit(visitorId);

        return StandardResponse.ok(null);
    }

    @GetMapping("/api/visitors")
    public ResponseEntity<StandardResponse<VisitorStatsDto>> getVisitorStats() {
        return StandardResponse.ok(visitorService.getVisitorStats());
    }
}
