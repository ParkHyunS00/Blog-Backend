package com.parkhyuns00.blog.domain.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AdminAuthAttemptTest {

    @Test
    @DisplayName("초기 상태는 잠기지 않은 상태다.")
    void test_initial_is_not_locked() {
        AdminAuthAttempt attempt = AdminAuthAttempt.initial();

        assertThat(attempt.isLocked()).isFalse();
    }

    @Test
    @DisplayName("OTP 실패 횟수가 4회 이하면 잠기지 않는다.")
    void test_not_locked_until_four_failures() {
        AdminAuthAttempt attempt = AdminAuthAttempt.initial();

        for (int i = 0; i < 4; i++) {
            attempt.recordFailure();
        }

        assertThat(attempt.isLocked()).isFalse();
    }

    @Test
    @DisplayName("OTP 실패 횟수가 5회가 되면 잠긴다.")
    void test_locked_when_five_failures() {
        AdminAuthAttempt attempt = AdminAuthAttempt.initial();

        for (int i = 0; i < 5; i++) {
            attempt.recordFailure();
        }

        assertThat(attempt.isLocked()).isTrue();
    }

    @Test
    @DisplayName("잠긴 상태에서 reset을 호출하면 잠금이 해제된다.")
    void test_reset_when_locked() {
        AdminAuthAttempt attempt = AdminAuthAttempt.initial();

        for (int i = 0; i < 5; i++) {
            attempt.recordFailure();
        }

        assertThat(attempt.isLocked()).isTrue();

        attempt.reset();

        assertThat(attempt.isLocked()).isFalse();
    }

    @Test
    @DisplayName("reset 이후 다시 5회 이상 실패하면 다시 잠긴다.")
    void test_locked_again_after_reset_with_five_failures() {
        AdminAuthAttempt attempt = AdminAuthAttempt.initial();

        for (int i = 0; i < 5; i++) {
            attempt.recordFailure();
        }

        attempt.reset();

        for (int i = 0; i < 5; i++) {
            attempt.recordFailure();
        }

        assertThat(attempt.isLocked()).isTrue();
    }
}
