package com.parkhyuns00.blog.config.security.provider;

import com.parkhyuns00.blog.config.security.dto.AdminProperties;
import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import com.parkhyuns00.blog.config.security.token.AdminKeyAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminKeyAuthenticationProvider implements AuthenticationProvider {

    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Object credentials = authentication.getCredentials();

        if (!(credentials instanceof String adminKey) || adminKey.isBlank()) {
            throw new AuthenticationServiceException("Admin key not found");
        }

        boolean matched = passwordEncoder.matches(adminKey, adminProperties.accessKeyHash());
        if (!matched) {
            throw new BadCredentialsException("Admin key is invalid");
        }

        AdminPrincipal principal = new AdminPrincipal(AdminRole.PRE_ADMIN);
        return new AdminKeyAuthenticationToken(principal, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AdminKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
