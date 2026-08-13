package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

@Testcontainers
@SpringBootTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
@ActiveProfiles("test")
public class PostFullTextSearchRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("게시글 제목과 본문에 Full Text Index 를 생성한다.")
    void test_create_post_fts_success() {
        List<String> indexNames = jdbcTemplate.queryForList("""
              SELECT DISTINCT INDEX_NAME
              FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'posts'
                AND INDEX_TYPE = 'FULLTEXT'
              """, String.class);

        assertThat(indexNames).contains("ft_posts_title", "ft_posts_content");
    }
}
