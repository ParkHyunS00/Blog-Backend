package com.parkhyuns00.blog.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.controller.dto.PostCreateRequest;
import com.parkhyuns00.blog.domain.post.controller.dto.PostDraftCreateRequest;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
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

import java.util.List;

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
    @DisplayName("빈 초안을 발행하고 수정하면 발행 컬럼에만 제목과 본문이 저장된다.")
    void test_persist_draft_publication_and_published_update() {
        Long postId = postService.createDraft(new PostDraftCreateRequest(
            null, null, null, null, null, null, null
        )).postId();

        entityManager.flush();
        entityManager.clear();

        Post draft = postRepository.findById(postId).orElseThrow();
        assertThat(draft.getTitle()).isNull();
        assertThat(draft.getContent()).isNull();
        assertThat(draft.getPublishedTitle()).isNull();
        assertThat(draft.getPublishedContent()).isNull();
        assertThat(draft.getSummary()).isEmpty();
        assertThat(postService.getDraftPost(postId).title()).isEmpty();
        assertThat(postService.getDraftPost(postId).content()).isEmpty();
        assertThat(postService.getDraftPosts(0).getContent().getFirst().title()).isEmpty();

        postService.publishDraft(postId, new PostCreateRequest(
            "발행 제목", "요약", "<p>발행 본문</p>", "Backend", List.of(), null, List.of()
        ));

        entityManager.flush();
        entityManager.clear();

        Post published = postRepository.findById(postId).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(published.getTitle()).isNull();
        assertThat(published.getContent()).isNull();
        assertThat(published.getPublishedTitle()).isEqualTo("발행 제목");
        assertThat(published.getPublishedContent()).isEqualTo("<p>발행 본문</p>");

        postService.updatePublishedPost(postId, new PostCreateRequest(
            "수정 제목", "수정 요약", "<p>수정 본문</p>", "Backend", List.of(), null, List.of()
        ));

        entityManager.flush();
        entityManager.clear();

        Post updated = postRepository.findById(postId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(updated.getTitle()).isNull();
        assertThat(updated.getContent()).isNull();
        assertThat(updated.getPublishedTitle()).isEqualTo("수정 제목");
        assertThat(updated.getPublishedContent()).isEqualTo("<p>수정 본문</p>");
        assertThat(updated.getSummary()).isEqualTo("수정 요약");
    }

    @Test
    @DisplayName("이미지와 태그가 연결된 발행 게시글을 삭제한다.")
    void test_delete_published_post_with_associations_success() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        Tag tag = tagRepository.save(new Tag("Java", "java"));

        Post post = postRepository.save(Post.publish("게시글 제목", "게시글 요약", "<p>게시글 본문</p>", category));
        PostImage image = new PostImage(PostImageType.CONTENT, "posts/content/delete-test.png", "image/png", 1200, 630);
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
