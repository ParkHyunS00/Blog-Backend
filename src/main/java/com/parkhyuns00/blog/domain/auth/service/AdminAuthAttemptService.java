package com.parkhyuns00.blog.domain.auth.service;

import com.parkhyuns00.blog.domain.auth.model.AdminAuthAttempt;
import com.parkhyuns00.blog.domain.auth.repository.AdminAuthAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthAttemptService {

    private static final int ID = 1;

    private final AdminAuthAttemptRepository adminAuthAttemptRepository;

    public boolean isLocked() {
        return adminAuthAttemptRepository.findById(ID)
            .map(AdminAuthAttempt::isLocked)
            .orElse(false);
    }

    @Transactional
    public void recordFailure() {
        AdminAuthAttempt attempt = findOrCreateDefaultAttempt();
        attempt.recordFailure();
    }

    @Transactional
    public void reset() {
        AdminAuthAttempt attempt = findOrCreateDefaultAttempt();
        attempt.reset();
    }

    private AdminAuthAttempt findOrCreateDefaultAttempt() {
        return adminAuthAttemptRepository.findById(ID)
            .orElseGet(() -> adminAuthAttemptRepository.save(AdminAuthAttempt.initial()));
    }
}
