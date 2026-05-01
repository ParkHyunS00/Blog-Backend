package com.parkhyuns00.blog.config.security.exception;

import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;

@Getter
public class AdminOtpAuthenticationException extends BadCredentialsException {

    private final int otpFailCount;
    private final boolean locked;

    public AdminOtpAuthenticationException(int otpFailCount, boolean locked) {
        super("OTP is invalid");
        this.otpFailCount = otpFailCount;
        this.locked = locked;
    }
}
