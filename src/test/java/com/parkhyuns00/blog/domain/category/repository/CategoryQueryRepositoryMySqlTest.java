package com.parkhyuns00.blog.domain.category.repository;

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
    "spring.test.database.replace=NONE"
})
class CategoryQueryRepositoryMySqlTest extends CategoryQueryRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("카테고리별 최신글 조회용 복합 인덱스를 생성한다.")
    void test_create_category_status_created_index() {
        var columns = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'posts'
              AND INDEX_NAME = 'idx_posts_category_status_created'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertThat(columns).containsExactly("category_id", "status", "created_at");
    }
}
