package com.parkhyuns00.blog.config.security.filter;

import com.parkhyuns00.blog.config.security.filter.dto.AdminOtpLoginRequest;
import com.parkhyuns00.blog.config.security.token.AdminOtpAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

public final class AdminOtpAuthenticationFilter
    extends AbstractAdminLoginFilter<AdminOtpLoginRequest, AdminOtpAuthenticationToken> {

    private static final String ADMIN_OTP_LOGIN_URI = "/api/admin/auth/otp";

    public AdminOtpAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        super(ADMIN_OTP_LOGIN_URI, authenticationManager, objectMapper);
    }

    @Override
    protected Class<AdminOtpLoginRequest> requestType() {
        return AdminOtpLoginRequest.class;
    }

    @Override
    protected void validate(AdminOtpLoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.otpCode() == null || loginRequest.otpCode().isBlank()) {
            throw new BadCredentialsException("OTP is required");
        }
    }

    @Override
    protected AdminOtpAuthenticationToken toToken(AdminOtpLoginRequest loginRequest) {
        return new AdminOtpAuthenticationToken(loginRequest.otpCode());
    }
}
