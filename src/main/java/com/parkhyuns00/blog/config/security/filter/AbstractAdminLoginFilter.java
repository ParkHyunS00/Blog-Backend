package com.parkhyuns00.blog.config.security.filter;

import com.parkhyuns00.blog.config.security.token.AdminAuthenticationToken;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public abstract sealed class AbstractAdminLoginFilter<R, T extends AdminAuthenticationToken>
    extends AbstractAuthenticationProcessingFilter
    permits AdminKeyAuthenticationFilter, AdminOtpAuthenticationFilter {

    private final ObjectMapper objectMapper;

    protected AbstractAdminLoginFilter(
        String uri,
        AuthenticationManager authenticationManager,
        ObjectMapper objectMapper
    ) {
        super(
            PathPatternRequestMatcher
                .withDefaults()
                .matcher(HttpMethod.POST, uri)
        );
        setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
    }

    @Override
    public @Nullable Authentication attemptAuthentication(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws AuthenticationException, IOException {
        validateContentType(request);

        R loginRequest = objectMapper.readValue(request.getInputStream(), requestType());
        validate(loginRequest);

        T token = toToken(loginRequest);
        token.setDetails(authenticationDetailsSource.buildDetails(request));

        return getAuthenticationManager().authenticate(token);
    }

    protected abstract Class<R> requestType();

    protected abstract void validate(R loginRequest);

    protected abstract T toToken(R loginRequest);

    private void validateContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            throw new AuthenticationServiceException("Content-Type must be application/json");
        }
    }
}
