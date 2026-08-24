package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostTag;
import com.parkhyuns00.blog.domain.post.repository.PostImageRepository;
import com.parkhyuns00.blog.domain.post.repository.PostRepository;
import com.parkhyuns00.blog.domain.post.repository.PostTagRepository;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("이미지와 태그가 연결된 발행 게시글을 삭제한다.")
    void test_delete_published_post_with_associations_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag tag = tagRepository.save(new Tag("Java", "java"));

        Post post = postRepository.save(Post.publish("게시글 제목", "게시글 요약", "<p>게시글 본문</p>", category));
        PostImage image = new PostImage(
            PostImageType.CONTENT,
            "posts/content/delete-test.png",
            "image/png"
        );
        image.attachTo(post);

        postImageRepository.save(image);

        PostTag postTag = postTagRepository.save(new PostTag(post, tag));

        postRepository.flush();

        Long postId = post.getId();
        Long imageId = image.getId();
        Long postTagId = postTag.getId();

        entityManager.flush();
        entityManager.clear();

        postService.deletePublishedPost(postId);

        assertThat(postRepository.findById(postId)).isEmpty();
        assertThat(postImageRepository.findById(imageId)).isEmpty();
        assertThat(postTagRepository.findById(postTagId)).isEmpty();
    }
}
