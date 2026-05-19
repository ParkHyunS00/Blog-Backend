package com.parkhyuns00.blog.config.security.token;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class AdminOtpAuthenticationToken extends AdminAuthenticationToken {

    public AdminOtpAuthenticationToken(Object otpCode) {
        super(otpCode);
    }

    public AdminOtpAuthenticationToken(
        Object principal,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(principal, authorities);
    }
}
