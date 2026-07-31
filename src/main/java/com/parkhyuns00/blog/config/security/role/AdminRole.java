package com.parkhyuns00.blog.config.security.role;

public enum AdminRole {
    PRE_ADMIN,
    ADMIN;

    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
