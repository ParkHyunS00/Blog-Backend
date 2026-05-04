package com.parkhyuns00.blog.config.security.handler;

import com.parkhyuns00.blog.config.security.filter.dto.AdminAuthStatus;
import com.parkhyuns00.blog.config.security.handler.dto.AdminAuthResponse;
import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AdminAuthResponseWriter writer;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();

        AdminAuthResponse body = switch (principal.role()) {
            case PRE_ADMIN -> new AdminAuthResponse(AdminAuthStatus.OTP_REQUIRED);
            case ADMIN -> new AdminAuthResponse(AdminAuthStatus.AUTHENTICATED);
        };

        log.info("[Admin authentication] succeeded, role={}, ip={}", principal.role(), request.getRemoteAddr());

        writer.write(response, StandardResponse.ok(body));
    }
}
