package com.parkhyuns00.blog.config.security.principal;

import com.parkhyuns00.blog.config.security.role.AdminRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AdminPrincipal(AdminRole role) implements UserDetails {

    private static final String USERNAME = "ADMIN";

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.toAuthority()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return USERNAME;
    }
}
