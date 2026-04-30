package com.parkhyuns00.blog.domain.auth.repository;

import com.parkhyuns00.blog.domain.auth.model.AdminAuthAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuthAttemptRepository extends JpaRepository<AdminAuthAttempt, Integer> {
}
