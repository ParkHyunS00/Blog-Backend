package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@DataJpaTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.test.database.replace=NONE",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.parkhyuns00.blog.domain.post.repository.PostQueryRepositoryTest$SqlCapture"
})
class PostQueryRepositoryMySqlTest extends PostQueryRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("일반 목록 조회용 복합 인덱스의 컬럼 순서와 내림차순을 검증한다.")
    void test_create_status_created_id_index() {
        var columns = jdbcTemplate.queryForList("""
            SELECT CONCAT(COLUMN_NAME, ':', COLLATION)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'posts'
              AND INDEX_NAME = 'idx_posts_status_created_id'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertThat(columns).containsExactly("status:A", "created_at:D", "id:D");
    }
}
