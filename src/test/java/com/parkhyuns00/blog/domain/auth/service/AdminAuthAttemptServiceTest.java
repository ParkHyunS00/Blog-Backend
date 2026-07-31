package com.parkhyuns00.blog.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.auth.repository.AdminAuthAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
public class AdminAuthAttemptServiceTest {

    @Autowired
    private AdminAuthAttemptService adminAuthAttemptService;

    @Autowired
    private AdminAuthAttemptRepository adminAuthAttemptRepository;

    @BeforeEach
    void setUp() {
        adminAuthAttemptRepository.deleteAll();
    }

    @Test
    @DisplayName("인증 시도 정보가 없으면 잠기지 않은 상태로 판단한다.")
    void test_locked_returns_false_then_attempt_not_exist() {
        boolean locked = adminAuthAttemptService.isLocked();

        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("OTP 실패를 기록하면 인증 시도 정보가 생성된다.")
    void test_record_failure_create_attempt() {
        adminAuthAttemptService.recordFailure();

        assertThat(adminAuthAttemptRepository.findById(1)).isPresent();
        assertThat(adminAuthAttemptService.isLocked()).isFalse();
    }

    @Test
    @DisplayName("OTP 실패를 5회 기록하면 잠긴 상태가 된다.")
    void test_record_failure_when_five_attempt_failures() {
        for (int i = 0; i < 5; i++) {
            adminAuthAttemptService.recordFailure();
        }

        assertThat(adminAuthAttemptService.isLocked()).isTrue();
    }

    @Test
    @DisplayName("잠긴 상태에서 reset을 호출하면 잠금이 해제된다.")
    void test_reset_unlock_attempt() {
        for (int i = 0; i < 5; i++) {
            adminAuthAttemptService.recordFailure();
        }

        assertThat(adminAuthAttemptService.isLocked()).isTrue();

        adminAuthAttemptService.reset();

        assertThat(adminAuthAttemptService.isLocked()).isFalse();
    }

    @Test
    @DisplayName("인증 시도 정보가 없을 때 reset을 호출하면 초기 상태 정보가 생성된다.")
    void test_reset_create_attempt_when_attempt_not_exist() {
        adminAuthAttemptService.reset();

        assertThat(adminAuthAttemptRepository.findById(1)).isPresent();
        assertThat(adminAuthAttemptService.isLocked()).isFalse();
    }
}
