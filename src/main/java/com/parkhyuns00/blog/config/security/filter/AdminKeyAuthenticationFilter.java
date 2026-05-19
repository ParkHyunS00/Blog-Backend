package com.parkhyuns00.blog.config.security.filter;

import com.parkhyuns00.blog.config.security.filter.dto.AdminKeyLoginRequest;
import com.parkhyuns00.blog.config.security.token.AdminKeyAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;


@Slf4j
public final class AdminKeyAuthenticationFilter
    extends AbstractAdminLoginFilter<AdminKeyLoginRequest, AdminKeyAuthenticationToken> {

    private static final String ADMIN_KEY_LOGIN_URI = "/api/admin/auth/key";

    public AdminKeyAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        super(ADMIN_KEY_LOGIN_URI, authenticationManager, objectMapper);
    }

    @Override
    protected Class<AdminKeyLoginRequest> requestType() {
        return AdminKeyLoginRequest.class;
    }

    @Override
    protected void validate(AdminKeyLoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.adminKey() == null || loginRequest.adminKey().isBlank()) {
            throw new BadCredentialsException("Admin key is required");
        }
    }

    @Override
    protected AdminKeyAuthenticationToken toToken(AdminKeyLoginRequest loginRequest) {
        return new AdminKeyAuthenticationToken(loginRequest.adminKey());
    }
}
