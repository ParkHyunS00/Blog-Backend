package com.parkhyuns00.blog.config.security.token;

import jakarta.annotation.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public abstract sealed class AdminAuthenticationToken extends AbstractAuthenticationToken
    permits AdminKeyAuthenticationToken, AdminOtpAuthenticationToken {

    private final Object principal;
    private final Object credentials;

    /**
     * 인증 전 생성자 (필터에서 호출)
     * 인증 전이므로 Principal은 없음
     */
    protected AdminAuthenticationToken(Object credentials) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = null;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    /**
     * 인증 완료 상태의 토큰을 생성
     * 인증 후 이므로 credentials 필요 없음
     * @param principal 인증된 관리자 주체
     * @param authorities 인증된 관리자에게 부여할 권한
     */
    protected AdminAuthenticationToken(
        Object principal,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        setAuthenticated(true);
    }

    @Override
    public @Nullable Object getCredentials() {
        return credentials;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return principal;
    }
}
