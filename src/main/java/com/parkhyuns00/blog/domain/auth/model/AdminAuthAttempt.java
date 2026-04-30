package com.parkhyuns00.blog.domain.auth.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_auth_attempt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuthAttempt {

    private static final int DEFAULT_FAIL_COUNT = 0;
    private static final int LOCK_THRESHOLD = 5;
    private static final int ADMIN_ID = 1;

    @Id
    private Integer id;

    @Column(name = "otp_fail_count")
    private Integer otpFailCount = DEFAULT_FAIL_COUNT;

    private Boolean locked;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    public static AdminAuthAttempt initial() {
        AdminAuthAttempt attempt = new AdminAuthAttempt();
        attempt.id = ADMIN_ID;
        return attempt;
    }

    public void recordFailure() {
        this.otpFailCount++;
        this.lastFailedAt = LocalDateTime.now();
        if (this.otpFailCount >= LOCK_THRESHOLD) {
            this.locked = Boolean.TRUE;
        }
    }

    public void reset() {
        this.otpFailCount = DEFAULT_FAIL_COUNT;
        this.lastFailedAt = null;
        this.locked = null;
    }

    public boolean isLocked() {
        return Boolean.TRUE.equals(locked);
    }
}
