package com.parkhyuns00.blog.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import({
    QueryDslConfig.class,
    JpaConfig.class
})
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("게시글의 조회수를 원자적으로 1 증가시킨다.")
    void test_increment_published_post_view_count_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Post post = postRepository.save(Post.publish("title", "summary", "content", category));

        int affectedRows = postRepository.incrementViewCount(post.getId());

        entityManager.flush();
        entityManager.clear();

        Post foundPost = postRepository.findById(post.getId()).orElseThrow();

        assertThat(affectedRows).isEqualTo(1);
        assertThat(foundPost.getViewCount()).isEqualTo(1L);
    }

}
