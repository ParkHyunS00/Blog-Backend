package com.parkhyuns00.blog.config.security.provider;

import com.parkhyuns00.blog.config.security.dto.AdminProperties;
import com.parkhyuns00.blog.config.security.exception.AdminExceptionCode;
import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import com.parkhyuns00.blog.config.security.token.AdminOtpAuthenticationToken;
import com.parkhyuns00.blog.domain.auth.service.AdminAuthAttemptService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AdminOtpAuthenticationProvider implements AuthenticationProvider {

    private static final Pattern OTP_PATTERN = Pattern.compile("\\d{6}");

    private final AdminProperties adminProperties;
    private final GoogleAuthenticator googleAuthenticator;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
        SecurityContextHolder.getContextHolderStrategy();
    private final AdminAuthAttemptService adminAuthAttemptService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (adminAuthAttemptService.isLocked()) {
            throw new LockedException(AdminExceptionCode.ADMIN_OTP_LOCKED.getMessage());
        }

        validatePreAdmin();

        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof String otpCode) || !OTP_PATTERN.matcher(otpCode).matches()) {
            adminAuthAttemptService.recordFailure();
            throw new BadCredentialsException("OTP is invalid");
        }

        boolean authorized = googleAuthenticator.authorize(adminProperties.otpSecret(), Integer.parseInt(otpCode));
        if (!authorized) {
            adminAuthAttemptService.recordFailure();
            throw new BadCredentialsException("OTP is invalid");
        }

        adminAuthAttemptService.reset();

        AdminPrincipal principal = new AdminPrincipal(AdminRole.ADMIN);
        return new AdminOtpAuthenticationToken(principal, principal.getAuthorities());
    }

    private void validatePreAdmin() {
        Authentication authentication = securityContextHolderStrategy.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("PRE_ADMIN authentication is required");
        }

        if (!(authentication.getPrincipal() instanceof AdminPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("PRE_ADMIN authentication is required");
        }

        if (principal.role() != AdminRole.PRE_ADMIN) {
            throw new AuthenticationCredentialsNotFoundException("PRE_ADMIN authentication is required");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AdminOtpAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
