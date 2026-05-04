package com.parkhyuns00.blog.domain.auth.controller;

import com.parkhyuns00.blog.domain.auth.controller.dto.AdminAuthStatusResponse;
import com.parkhyuns00.blog.domain.auth.controller.dto.AdminAuthStep;
import com.parkhyuns00.blog.global.response.StandardResponse;
import com.parkhyuns00.blog.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/admin")
@RestController
public class AdminAuthController {

    @GetMapping("/csrf")
    public ResponseEntity<StandardResponse<Void>> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return StandardResponse.ok(null);
    }

    @GetMapping("/auth/status")
    public ResponseEntity<StandardResponse<AdminAuthStatusResponse>> status() {
        AdminAuthStatusResponse response = SecurityUtil.getCurrentAdminRole()
            .map(role -> switch(role) {
                case PRE_ADMIN -> new AdminAuthStatusResponse(false, AdminAuthStep.OTP_REQUIRED);
                case ADMIN -> new AdminAuthStatusResponse(true, AdminAuthStep.AUTHENTICATED);
            })
            .orElseGet(() -> new AdminAuthStatusResponse(false, AdminAuthStep.ADMIN_KEY_REQUIRED));

        return StandardResponse.ok(response);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<StandardResponse<Void>> logout(HttpServletRequest request) {
        SecurityUtil.clearAuthentication(request);

        log.info("[Admin authentication] logout, ip={}", request.getRemoteAddr());
        return StandardResponse.ok(null);
    }
}
