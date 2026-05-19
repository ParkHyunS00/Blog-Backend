package com.parkhyuns00.blog.config.security.handler;

import com.parkhyuns00.blog.global.response.StandardResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AdminAuthResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(
        HttpServletResponse response,
        ResponseEntity<? extends StandardResponse<?>> responseEntity
    ) throws IOException {
        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), responseEntity.getBody());
    }
}
