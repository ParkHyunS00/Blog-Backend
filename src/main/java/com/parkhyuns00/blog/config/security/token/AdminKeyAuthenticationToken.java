package com.parkhyuns00.blog.config.security.token;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public final class AdminKeyAuthenticationToken extends AdminAuthenticationToken {

    public AdminKeyAuthenticationToken(Object adminKey) {
        super(adminKey);
    }

    public AdminKeyAuthenticationToken(
        Object principal,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(principal, authorities);
    }
}
