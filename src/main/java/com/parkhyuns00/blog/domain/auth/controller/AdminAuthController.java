package com.parkhyuns00.blog.domain.auth.controller;

import com.parkhyuns00.blog.global.response.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {

    @GetMapping("/api/csrf")
    public ResponseEntity<StandardResponse<Void>> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return StandardResponse.ok(null);
    }
}
