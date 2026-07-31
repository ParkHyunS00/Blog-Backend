package com.parkhyuns00.blog.security.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.parkhyuns00.blog.config.security.dto.AdminProperties;
import com.parkhyuns00.blog.config.security.principal.AdminPrincipal;
import com.parkhyuns00.blog.config.security.provider.AdminKeyAuthenticationProvider;
import com.parkhyuns00.blog.config.security.role.AdminRole;
import com.parkhyuns00.blog.config.security.token.AdminKeyAuthenticationToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AdminKeyAuthenticationProviderTest {

    @Mock
    private AdminProperties adminProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminKeyAuthenticationProvider provider;

    @Test
    @DisplayName("관리자 키가 일치하면 PRE_ADMIN 인증 토큰을 발급한다.")
    void test_authenticate_success() {
        String rawAdminKey = "test-admin-key";
        String encodedAdminKey = "encoded";

        when(adminProperties.accessKeyHash()).thenReturn(encodedAdminKey);
        when(passwordEncoder.matches(rawAdminKey, encodedAdminKey)).thenReturn(true);

        AdminKeyAuthenticationToken unAuthenticated = new AdminKeyAuthenticationToken(rawAdminKey);

        Authentication authentication = provider.authenticate(unAuthenticated);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication).isInstanceOf(AdminKeyAuthenticationToken.class);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AdminPrincipal.class);

        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
        assertThat(principal.role()).isEqualTo(AdminRole.PRE_ADMIN);
    }

    @Test
    @DisplayName("관리자 키가 일치하지 않으면 BadCredentialsException 예외가 발생한다.")
    void test_authenticate_fail_with_invalid_admin_key() {
        String rawAdminKey = "test-admin-key";
        String encodedAdminKey = "encoded";

        when(adminProperties.accessKeyHash()).thenReturn(encodedAdminKey);
        when(passwordEncoder.matches(rawAdminKey, encodedAdminKey)).thenReturn(false);

        AdminKeyAuthenticationToken unAuthenticated = new AdminKeyAuthenticationToken(rawAdminKey);

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Admin key is invalid");
    }

    @Test
    @DisplayName("관리자 키가 비어있으면 AuthenticationServiceException 예외가 발생한다.")
    void test_authenticate_fail_with_blank_admin_key() {
        AdminKeyAuthenticationToken unAuthenticated = new AdminKeyAuthenticationToken(" ");

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(AuthenticationServiceException.class)
            .hasMessage("Admin key not found");
    }

    @Test
    @DisplayName("credentials 가 null 이면 AuthenticationServiceException 예외가 발생한다.")
    void test_authenticate_fail_with_null_credentials() {
        AdminKeyAuthenticationToken unAuthenticated = new AdminKeyAuthenticationToken(null);

        assertThatThrownBy(() -> provider.authenticate(unAuthenticated))
            .isInstanceOf(AuthenticationServiceException.class)
            .hasMessage("Admin key not found");
    }

    @Test
    @DisplayName("AdminKeyAuthenticationToken 을 지원한다")
    void test_supports_admin_key_authentication_token() {
        assertThat(provider.supports(AdminKeyAuthenticationToken.class)).isTrue();
    }
}
