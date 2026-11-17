package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class PostPublishedContentMigrationTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @Test
    @DisplayName("기존 발행글의 제목과 본문을 이관하고 초안을 보존하며 상태별 저장 규칙을 보장한다.")
    void test_migrate_existing_posts_to_published_content_columns() {
        Flyway.configure()
            .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
            .target("10")
            .load()
            .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()
        ));

        jdbcTemplate.update("""
            INSERT INTO categories (id, name, slug, created_at, updated_at)
            VALUES (1, 'Backend', 'backend', '2026-09-01 12:00:00', '2026-09-01 12:00:00')
            """);
        jdbcTemplate.update("""
            INSERT INTO posts
                (id, title, summary, content, category_id, status, view_count, created_at, updated_at)
            VALUES
                (1, '스프링 발행글', '발행 요약', '<p>스프링 공개 본문</p>', 1, 'PUBLISHED', 7,
                    '2026-09-01 12:00:00', '2026-09-02 12:00:00'),
                (2, '리눅스 서버', '서버 요약', '<p>네트워크 본문</p>', 1, 'PUBLISHED', 0,
                    '2026-09-01 12:00:00', '2026-09-01 12:00:00'),
                (3, '스프링 초안', '초안 요약', '<p>스프링 비공개 본문</p>', 1, 'DRAFT', 0,
                    '2026-09-01 12:00:00', '2026-09-01 12:00:00'),
                (4, '', '', '', NULL, 'DRAFT', 0,
                    '2026-09-01 12:00:00', '2026-09-01 12:00:00')
            """);

        Flyway.configure()
            .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
            .load()
            .migrate();

        assertThat(jdbcTemplate.queryForMap("SELECT * FROM posts WHERE id = 1"))
            .containsEntry("title", null)
            .containsEntry("content", null)
            .containsEntry("published_title", "스프링 발행글")
            .containsEntry("published_content", "<p>스프링 공개 본문</p>")
            .containsEntry("summary", "발행 요약")
            .containsEntry("status", "PUBLISHED")
            .containsEntry("view_count", 7L);

        assertThat(jdbcTemplate.queryForMap("SELECT * FROM posts WHERE id = 3"))
            .containsEntry("title", "스프링 초안")
            .containsEntry("content", "<p>스프링 비공개 본문</p>")
            .containsEntry("published_title", null)
            .containsEntry("published_content", null)
            .containsEntry("summary", "초안 요약")
            .containsEntry("status", "DRAFT");

        assertThat(jdbcTemplate.queryForMap("SELECT * FROM posts WHERE id = 4"))
            .containsEntry("title", "")
            .containsEntry("content", "")
            .containsEntry("published_title", null)
            .containsEntry("published_content", null)
            .containsEntry("category_id", null);

        assertThat(jdbcTemplate.queryForList("""
            SELECT id
            FROM posts
            WHERE MATCH(published_title, published_content)
                AGAINST ('스프링' IN NATURAL LANGUAGE MODE)
            """, Long.class)).containsExactly(1L);
        assertThat(jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM posts
            WHERE MATCH(published_title, published_content)
                AGAINST ('스프링' IN NATURAL LANGUAGE MODE)
            """, Long.class)).isEqualTo(1L);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE posts SET published_title = '초안이 노출되면 안 됨' WHERE id = 3"
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("chk_posts_content_by_status");
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE posts SET published_content = '초안이 노출되면 안 됨' WHERE id = 3"
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("chk_posts_content_by_status");
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE posts SET title = '잘못된 발행 위치' WHERE id = 1"
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("chk_posts_content_by_status");
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE posts SET published_content = NULL WHERE id = 1"
        )).isInstanceOf(DataAccessException.class).hasMessageContaining("chk_posts_content_by_status");

        assertThat(jdbcTemplate.update("UPDATE posts SET title = NULL, content = NULL WHERE id = 4"))
            .isEqualTo(1);
        assertThat(jdbcTemplate.queryForMap("SELECT title, content, summary FROM posts WHERE id = 4"))
            .containsEntry("title", null)
            .containsEntry("content", null)
            .containsEntry("summary", "");
    }
}
