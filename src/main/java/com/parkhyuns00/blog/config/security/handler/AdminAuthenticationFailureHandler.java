package com.parkhyuns00.blog.config.security.handler;

import com.parkhyuns00.blog.config.security.exception.AdminExceptionCode;
import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AdminAuthResponseWriter responseWriter;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        log.warn("Admin authentication failed, reason={}", exception.getMessage());

        responseWriter.write(
            response,
            StandardResponse.fail(AdminExceptionCode.ADMIN_AUTHENTICATION_FAILED)
        );
    }
}
