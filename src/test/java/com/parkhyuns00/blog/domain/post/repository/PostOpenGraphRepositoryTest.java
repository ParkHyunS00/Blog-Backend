package com.parkhyuns00.blog.domain.post.repository;

import com.parkhyuns00.blog.config.jpa.JpaConfig;
import com.parkhyuns00.blog.config.querydsl.QueryDslConfig;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.post.model.Post;
import com.parkhyuns00.blog.domain.post.model.PostImage;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.service.dto.PostOpenGraphDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@Import({QueryDslConfig.class, JpaConfig.class})
class PostOpenGraphRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("공개 게시글의 발행 제목과 실제 썸네일 메타데이터를 조회한다.")
    void test_find_published_metadata_with_thumbnail() {
        Post post = savePublishedPost();
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/image.webp", "image/webp", 800, 450);
        thumbnail.attachTo(post);
        postImageRepository.save(thumbnail);

        PostOpenGraphDto metadata = postRepository.findPublishedPostOpenGraphById(post.getId()).orElseThrow();

        assertThat(metadata).isEqualTo(new PostOpenGraphDto(
            post.getId(), "발행 제목", "게시글 요약", thumbnail.getId(), "image/webp", 800, 450
        ));
        assertThat(post.getTitle()).isNull();
    }

    @Test
    @DisplayName("본문 이미지, 미연결 이미지, 다른 글의 썸네일은 OG 썸네일로 사용하지 않는다.")
    void test_find_published_metadata_without_valid_thumbnail() {
        Post post = savePublishedPost();
        Post anotherPost = postRepository.save(Post.publish("다른 제목", "다른 요약", "본문", post.getCategory()));
        PostImage content = new PostImage(PostImageType.CONTENT, "posts/content/image.png", "image/png", 400, 300);
        content.attachTo(post);
        PostImage orphan = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/orphan.png", "image/png", 800, 450);
        PostImage anotherThumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/another.png", "image/png", 800, 450);
        anotherThumbnail.attachTo(anotherPost);
        postImageRepository.saveAll(List.of(content, orphan, anotherThumbnail));

        PostOpenGraphDto metadata = postRepository.findPublishedPostOpenGraphById(post.getId()).orElseThrow();

        assertThat(metadata.thumbnailImageId()).isNull();
        assertThat(metadata.mimeType()).isNull();
        assertThat(metadata.width()).isNull();
        assertThat(metadata.height()).isNull();
    }

    @Test
    @DisplayName("저장 객체 키가 비어 있는 썸네일은 유효한 이미지로 반환하지 않는다.")
    void test_find_published_metadata_with_invalid_thumbnail_object_key() {
        Post post = savePublishedPost();
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "   ", "image/png", 800, 450);
        thumbnail.attachTo(post);
        postImageRepository.save(thumbnail);

        assertThat(postRepository.findPublishedPostOpenGraphById(post.getId()).orElseThrow().thumbnailImageId()).isNull();
    }

    @Test
    @DisplayName("이미지 MIME 타입이 아닌 썸네일은 유효한 이미지로 반환하지 않는다.")
    void test_find_published_metadata_with_invalid_thumbnail_mime_type() {
        Post post = savePublishedPost();
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/invalid.txt", "text/plain", 800, 450);
        thumbnail.attachTo(post);
        postImageRepository.save(thumbnail);

        assertThat(postRepository.findPublishedPostOpenGraphById(post.getId()).orElseThrow().thumbnailImageId()).isNull();
    }

    @Test
    @DisplayName("삭제된 썸네일은 공개 게시글의 OG 조회를 실패시키지 않는다.")
    void test_find_published_metadata_after_thumbnail_deleted() {
        Post post = savePublishedPost();
        PostImage thumbnail = new PostImage(PostImageType.THUMBNAIL, "posts/thumbnail/deleted.png", "image/png", 800, 450);
        thumbnail.attachTo(post);
        postImageRepository.saveAndFlush(thumbnail);
        postImageRepository.delete(thumbnail);
        postImageRepository.flush();

        assertThat(postRepository.findPublishedPostOpenGraphById(post.getId()).orElseThrow().thumbnailImageId()).isNull();
    }

    @Test
    @DisplayName("공개되지 않은 임시저장은 OG 조회에서 제외한다.")
    void test_find_published_metadata_excludes_draft() {
        Post draft = postRepository.save(Post.createDraft("비공개 초안", "요약", "본문", null));

        assertThat(postRepository.findPublishedPostOpenGraphById(draft.getId())).isEmpty();
    }

    @Test
    @DisplayName("삭제된 게시글은 OG 조회에서 제외한다.")
    void test_find_published_metadata_excludes_deleted_post() {
        Post post = savePublishedPost();
        Long postId = post.getId();
        postRepository.delete(post);
        postRepository.flush();

        assertThat(postRepository.findPublishedPostOpenGraphById(postId)).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 OG 조회 결과가 없다.")
    void test_find_published_metadata_missing_post() {
        assertThat(postRepository.findPublishedPostOpenGraphById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    @DisplayName("반복 OG 조회 전후 데이터베이스의 조회수가 유지된다.")
    void test_repeated_og_queries_do_not_increment_view_count() {
        Long postId = savePublishedPost().getId();
        postRepository.incrementViewCount(postId);
        postRepository.incrementViewCount(postId);
        entityManager.flush();
        entityManager.clear();
        long before = postRepository.findById(postId).orElseThrow().getViewCount();

        postRepository.findPublishedPostOpenGraphById(postId).orElseThrow();
        postRepository.findPublishedPostOpenGraphById(postId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        assertThat(before).isEqualTo(2L);
        assertThat(postRepository.findById(postId).orElseThrow().getViewCount()).isEqualTo(before);
    }

    private Post savePublishedPost() {
        Category category = categoryRepository.save(new Category("Backend", "backend"));
        return postRepository.save(Post.publish("발행 제목", "게시글 요약", "본문", category));
    }
}
