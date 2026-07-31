package com.parkhyuns00.blog.security.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.config.security.dto.AdminProperties;
import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.config.security.provider.AdminOtpAuthenticationProvider;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import com.parkhyuns00.blog.config.security.token.AdminKeyAuthenticationToken;
import com.parkhyuns00.blog.config.security.token.AdminOtpAuthenticationToken;
import com.parkhyuns00.blog.domain.auth.service.AdminAuthAttemptService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class AdminOtpAuthenticationProviderTest {

    @Mock
    private AdminProperties adminProperties;

    @Mock
    private GoogleAuthenticator googleAuthenticator;

    @Mock
    private AdminAuthAttemptService adminAuthAttemptService;

    @InjectMocks
    private AdminOtpAuthenticationProvider provider;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PRE_ADMIN 상태에서 올바른 OTP를 입력하면 ADMIN 권한으로 승격된다.")
    void test_authenticate_success() {
        setPreAdminAuthentication();

        String otpSecret = "TEST_OTP_SCERET";
        String otpCode = "123123";

        when(adminAuthAttemptService.isLocked()).thenReturn(false);
        when(adminProperties.otpSecret()).thenReturn(otpSecret);
        when(googleAuthenticator.authorize(otpSecret, Integer.parseInt(otpCode))).thenReturn(true);

        AdminOtpAuthenticationToken unAuthenticated = new AdminOtpAuthenticationToken(otpCode);

        Authentication authentication = provider.authenticate(unAuthenticated);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication).isInstanceOf(AdminOtpAuthenticationToken.class);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AdminPrincipal.class);

        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
        assertThat(principal.role()).isEqualTo(AdminRole.ADMIN);
        assertThat(principal.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");

        verify(adminAuthAttemptService).reset();
        verify(adminAuthAttemptService, never()).recordFailure();
    }

    @Test
    @DisplayName("OTP 인증이 잠긴 상태면 LockedException 예외가 발생한다.")
    void test_authenticate_fail_with_locked() {
        when(adminAuthAttemptService.isLocked()).thenReturn(true);

        AdminOtpAuthenticationToken unAuthenticated = new AdminOtpAuthenticationToken("123123");

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(LockedException.class);

        verify(adminAuthAttemptService, never()).recordFailure();
        verify(adminAuthAttemptService, never()).reset();
    }

    @Test
    @DisplayName("Security Context 에 인증 정보가 없으면 실패한다.")
    void test_authenticate_fail_without_authentication() {
        when(adminAuthAttemptService.isLocked()).thenReturn(false);

        AdminOtpAuthenticationToken unAuthenticated = new AdminOtpAuthenticationToken("123123");

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessage("PRE_ADMIN authentication is required");
    }

    @Test
    @DisplayName("현재 인증 시도가 PRE_ADMIN 권한이 아니면 실패한다.")
    void test_authenticate_fail_when_current_authentication_is_not_pre_admin() {
        setAdminAuthentication();

        when(adminAuthAttemptService.isLocked()).thenReturn(false);

        AdminOtpAuthenticationToken unAuthenticated = new AdminOtpAuthenticationToken("123123");

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
            .hasMessage("PRE_ADMIN authentication is required");

    }

    @Test
    @DisplayName("OTP Code가 6자리 숫자 형식이 아니면 실패 횟수를 기록하고 예외가 발생한다.")
    void test_authenticate_fail_with_invalid_otp_format() {
        setPreAdminAuthentication();

        when(adminAuthAttemptService.isLocked()).thenReturn(false);

        AdminOtpAuthenticationToken unAuthenticated = new AdminOtpAuthenticationToken("123123123");

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("OTP is invalid");
    }

    @Test
    @DisplayName("OTP가 일치하지 않으면 실패 횟수를 기록하고 예외가 발생한다.")
    void test_authenticate_fail_with_wrong_otp_code() {
        setPreAdminAuthentication();

        String otpSecret = "TEST_OTP_SCERET";
        String otpCode = "123123";

        when(adminAuthAttemptService.isLocked()).thenReturn(false);
        when(adminProperties.otpSecret()).thenReturn(otpSecret);
        when(googleAuthenticator.authorize(otpSecret, Integer.parseInt(otpCode))).thenReturn(false);

        AdminOtpAuthenticationToken unAuthenticated = new AdminOtpAuthenticationToken(otpCode);

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("OTP is invalid");

        verify(adminAuthAttemptService).recordFailure();
        verify(adminAuthAttemptService, never()).reset();
    }

    @Test
    @DisplayName("AdminOtpAuthenticationToken을 지원한다")
    void test_supports_admin_otp_authentication_token() {
        assertThat(provider.supports(AdminOtpAuthenticationToken.class)).isTrue();
    }

    private void setPreAdminAuthentication() {
        AdminPrincipal principal = new AdminPrincipal(AdminRole.PRE_ADMIN);
        Authentication authentication = new AdminKeyAuthenticationToken(principal, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setAdminAuthentication() {
        AdminPrincipal principal = new AdminPrincipal(AdminRole.ADMIN);
        Authentication authentication = new AdminOtpAuthenticationToken(principal, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
