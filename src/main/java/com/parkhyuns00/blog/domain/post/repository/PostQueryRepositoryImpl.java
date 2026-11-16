package com.parkhyuns00.blog.domain.post.repository;

import static com.parkhyuns00.blog.domain.category.model.QCategory.category;
import static com.parkhyuns00.blog.domain.post.model.QPost.post;
import static com.parkhyuns00.blog.domain.post.model.QPostImage.postImage;
import static com.parkhyuns00.blog.domain.post.model.QPostTag.postTag;
import static com.parkhyuns00.blog.domain.tag.model.QTag.tag;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.dto.*;
import com.parkhyuns00.blog.domain.post.service.dto.*;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Page<PostSummaryDto> findPublishedPosts(PostSearchCondition condition, Pageable pageable) {
        if (condition.hasKeyword()) {
            return findPublishedPostsByKeyword(
                condition.keyword(),
                pageable
            );
        }

        List<PostSummaryProjection> summaries = condition.hasCategory() || condition.hasTags()
            ? findFilteredPublishedPostSummaries(condition, pageable)
            : findPagedPublishedPostSummaries(pageable);

        JPAQuery<Long> countQuery = queryFactory
            .select(post.count())
            .from(post)
            .where(
                post.status.eq(PostStatus.PUBLISHED),
                hasAllTags(condition.tagSlugs())
            );

        if (condition.hasCategory()) {
            countQuery.join(post.category, category)
                .where(categoryEq(condition.categorySlug()));
        }

        Long total = countQuery.fetchOne();

        long totalElements = total == null ? 0 : total;

        return createSummaryPage(summaries, pageable, totalElements);
    }

    private List<PostSummaryProjection> findPagedPublishedPostSummaries(Pageable pageable) {
        String sql = """
            SELECT
                p.id AS post_id,
                p.published_title AS title,
                p.summary,
                pi.id AS thumbnail_image_id,
                c.name AS category_name,
                c.slug AS category_slug,
                p.created_at,
                p.updated_at
            FROM (
                SELECT p2.id
                FROM posts p2
                WHERE p2.status = 'PUBLISHED'
                ORDER BY p2.created_at DESC, p2.id DESC
                LIMIT :offset, :size
            ) page
            JOIN posts p ON p.id = page.id
            JOIN categories c ON c.id = p.category_id
            LEFT JOIN post_images pi
                ON pi.post_id = p.id
                AND pi.type = 'THUMBNAIL'
            ORDER BY p.created_at DESC, p.id DESC
            """;

        return entityManager.unwrap(Session.class)
            .createNativeQuery(sql, Object[].class)
            .addScalar("post_id", Long.class)
            .addScalar("title", String.class)
            .addScalar("summary", String.class)
            .addScalar("thumbnail_image_id", Long.class)
            .addScalar("category_name", String.class)
            .addScalar("category_slug", String.class)
            .addScalar("created_at", LocalDateTime.class)
            .addScalar("updated_at", LocalDateTime.class)
            .setParameter("offset", pageable.getOffset())
            .setParameter("size", pageable.getPageSize())
            .setTupleTransformer((row, _) -> new PostSummaryProjection(
                (Long) row[0],
                (String) row[1],
                (String) row[2],
                (Long) row[3],
                (String) row[4],
                (String) row[5],
                (LocalDateTime) row[6],
                (LocalDateTime) row[7]
            ))
            .getResultList();
    }

    private List<PostSummaryProjection> findFilteredPublishedPostSummaries(
        PostSearchCondition condition, Pageable pageable
    ) {
        return queryFactory
            .select(Projections.constructor(
                PostSummaryProjection.class,
                post.id,
                post.publishedTitle,
                post.summary,
                postImage.id,
                category.name,
                category.slug,
                post.createdAt,
                post.updatedAt
            ))
            .from(post)
            .join(post.category, category)
            .leftJoin(postImage)
            .on(
                postImage.post.eq(post),
                postImage.type.eq(PostImageType.THUMBNAIL)
            )
            .where(
                post.status.eq(PostStatus.PUBLISHED),
                categoryEq(condition.categorySlug()),
                hasAllTags(condition.tagSlugs())
            )
            .orderBy(post.createdAt.desc(), post.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    @Override
    public Optional<PostDetailDto> findPublishedPostById(Long postId) {
        PostDetailProjection detail = queryFactory
            .select(Projections.constructor(
                PostDetailProjection.class,
                post.id,
                post.publishedTitle,
                post.summary,
                post.publishedContent,
                post.viewCount,
                postImage.id,
                category.name,
                category.slug,
                post.createdAt,
                post.updatedAt
            ))
            .from(post)
            .join(post.category, category)
            .leftJoin(postImage)
            .on(
                postImage.post.eq(post),
                postImage.type.eq(PostImageType.THUMBNAIL)
            )
            .where(
                post.id.eq(postId),
                post.status.eq(PostStatus.PUBLISHED)
            )
            .fetchOne();

        if (detail == null) return Optional.empty();

        List<PostTagProjection> postTags = findPostTagsByPostId(postId);

        List<TagDto> tags = postTags.stream()
            .map(projection -> new TagDto(
                projection.tagId(),
                projection.name(),
                projection.slug()
            ))
            .toList();

        List<Long> contentImageIds = queryFactory
            .select(postImage.id)
            .from(postImage)
            .where(
                postImage.post.id.eq(postId),
                postImage.type.eq(PostImageType.CONTENT)
            )
            .orderBy(postImage.id.asc())
            .fetch();

        return Optional.of(new PostDetailDto(
            detail.postId(),
            detail.title(),
            detail.summary(),
            detail.content(),
            detail.viewCount(),
            detail.thumbnailImageId(),
            detail.categoryName(),
            detail.categorySlug(),
            tags,
            contentImageIds,
            detail.createdAt(),
            detail.updatedAt()
        ));
    }

    @Override
    public Page<PostDraftSummaryDto> findDraftPosts(Pageable pageable) {
        List<PostDraftSummaryProjection> summaries = queryFactory.select(
            Projections.constructor(
                PostDraftSummaryProjection.class,
                post.id,
                post.title,
                category.id,
                category.name,
                category.slug,
                post.updatedAt
            ))
            .from(post)
            .leftJoin(post.category, category)
            .where(post.status.eq(PostStatus.DRAFT))
            .orderBy(post.updatedAt.desc(), post.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(post.count())
            .from(post)
            .where(post.status.eq(PostStatus.DRAFT))
            .fetchOne();

        long totalElements = total == null ? 0 : total;

        if (summaries.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, totalElements);
        }

        List<Long> postIds = summaries.stream()
            .map(PostDraftSummaryProjection::postId)
            .toList();

        Map<Long, List<TagDto>> tagsByPostId = findTagsByPostIds(postIds);

        List<PostDraftSummaryDto> content = summaries.stream()
            .map(summary -> new PostDraftSummaryDto(
                summary.postId(),
                summary.title(),
                createCategoryDto(summary),
                tagsByPostId.getOrDefault(summary.postId(), List.of()),
                summary.updatedAt()
            ))
            .toList();

        return new PageImpl<>(content, pageable, totalElements);
    }

    @Override
    public Optional<PostDraftDetailDto> findDraftPostById(Long postId) {
        PostDraftDetailProjection detail = queryFactory.select(
            Projections.constructor(
                PostDraftDetailProjection.class,
                post.id,
                post.title,
                post.summary,
                post.content,
                category.id,
                category.name,
                category.slug,
                postImage.id,
                post.createdAt,
                post.updatedAt
            ))
            .from(post)
            .leftJoin(post.category, category)
            .leftJoin(postImage)
            .on(postImage.post.eq(post), postImage.type.eq(PostImageType.THUMBNAIL))
            .where(post.id.eq(postId), post.status.eq(PostStatus.DRAFT))
            .fetchOne();

        if (detail == null) return Optional.empty();

        List<PostTagProjection> postTags = findPostTagsByPostId(postId);

        List<TagDto> tags = postTags.stream()
            .map(projection -> new TagDto(projection.tagId(), projection.name(), projection.slug()))
            .toList();

        List<Long> contentImageIds = queryFactory
            .select(postImage.id)
            .from(postImage)
            .where(postImage.post.id.eq(postId), postImage.type.eq(PostImageType.CONTENT))
            .orderBy(postImage.id.asc())
            .fetch();

        CategoryDto categoryDto = detail.categoryId() == null ? null : new CategoryDto(detail.categoryId(), detail.categoryName(), detail.categorySlug());

        return Optional.of(
            new PostDraftDetailDto(
                detail.postId(),
                detail.title(),
                detail.summary(),
                detail.content(),
                categoryDto,
                tags,
                detail.thumbnailImageId(),
                contentImageIds,
                detail.createdAt(),
                detail.updatedAt()
            )
        );
    }

    private List<PostTagProjection> findPostTagsByPostId(Long postId) {
        return queryFactory.select(
            Projections.constructor(
                PostTagProjection.class,
                postTag.post.id,
                tag.id,
                tag.name,
                tag.slug
            ))
            .from(postTag)
            .join(postTag.tag, tag)
            .where(postTag.post.id.eq(postId))
            .orderBy(tag.name.asc(), tag.id.asc())
            .fetch();
    }

    private CategoryDto createCategoryDto(PostDraftSummaryProjection summary) {
        if (summary.categoryId() == null) return null;

        return new CategoryDto(summary.categoryId(), summary.categoryName(), summary.categorySlug());
    }

    private Map<Long, List<TagDto>> findTagsByPostIds(List<Long> postIds) {
        List<PostTagProjection> postTags = queryFactory
            .select(
                Projections.constructor(
                    PostTagProjection.class,
                    postTag.post.id,
                    tag.id,
                    tag.name,
                    tag.slug
                )
            )
            .from(postTag)
            .join(postTag.tag, tag)
            .where(postTag.post.id.in(postIds))
            .orderBy(tag.name.asc(), tag.id.asc())
            .fetch();

        return postTags.stream()
            .collect(
                Collectors.groupingBy(
                    PostTagProjection::postId,
                    Collectors.mapping(
                        projection -> new TagDto(projection.tagId(), projection.name(), projection.slug()),
                        Collectors.toList()
                    )
                ));
    }

    private Page<PostSummaryDto> findPublishedPostsByKeyword(String keyword, Pageable pageable) {
        String sql = """
            SELECT
                p.id AS post_id,
                p.published_title AS title,
                p.summary,
                pi.id AS thumbnail_image_id,
                c.name AS category_name,
                c.slug AS category_slug,
                p.created_at,
                p.updated_at
            FROM (
                SELECT
                    p2.id,
                    MATCH(p2.published_title, p2.published_content)
                        AGAINST (:keyword IN NATURAL LANGUAGE MODE) AS relevance
                FROM posts p2
                WHERE p2.status = 'PUBLISHED'
                    AND MATCH(p2.published_title, p2.published_content)
                        AGAINST (:keyword IN NATURAL LANGUAGE MODE) > 0
                ORDER BY relevance DESC
                LIMIT :offset, :size
            ) search_result
            JOIN posts p ON p.id = search_result.id
            JOIN categories c ON c.id = p.category_id
            LEFT JOIN post_images pi
                ON pi.post_id = p.id
                AND pi.type = 'THUMBNAIL'
            ORDER BY search_result.relevance DESC
            """;

        List<PostSummaryProjection> summaries = entityManager.unwrap(Session.class)
            .createNativeQuery(sql, Object[].class)
            .addScalar("post_id", Long.class)
            .addScalar("title", String.class)
            .addScalar("summary", String.class)
            .addScalar("thumbnail_image_id", Long.class)
            .addScalar("category_name", String.class)
            .addScalar("category_slug", String.class)
            .addScalar("created_at", LocalDateTime.class)
            .addScalar("updated_at", LocalDateTime.class)
            .setParameter("keyword", keyword)
            .setParameter("offset", pageable.getOffset())
            .setParameter("size", pageable.getPageSize())
            .setTupleTransformer((row, _) -> new PostSummaryProjection(
                (Long) row[0],
                (String) row[1],
                (String) row[2],
                (Long) row[3],
                (String) row[4],
                (String) row[5],
                (LocalDateTime) row[6],
                (LocalDateTime) row[7]
            ))
            .getResultList();

        long totalElements = countPublishedPostsByKeyword(keyword);

        return createSummaryPage(summaries, pageable, totalElements);
    }

    private Page<PostSummaryDto> createSummaryPage(
        List<PostSummaryProjection> summaries,
        Pageable pageable,
        long totalElements
    ) {
        if (summaries.isEmpty()) {
            return new PageImpl<>(
                List.of(),
                pageable,
                totalElements
            );
        }

        List<Long> postIds = summaries.stream()
            .map(PostSummaryProjection::postId)
            .toList();

        List<PostTagProjection> postTags = queryFactory
            .select(Projections.constructor(
                PostTagProjection.class,
                postTag.post.id,
                tag.id,
                tag.name,
                tag.slug
            ))
            .from(postTag)
            .join(postTag.tag, tag)
            .where(postTag.post.id.in(postIds))
            .orderBy(tag.name.asc(), tag.id.asc())
            .fetch();

        Map<Long, List<TagDto>> tagsByPostId = postTags.stream()
            .collect(Collectors.groupingBy(
                PostTagProjection::postId,
                Collectors.mapping(
                    projection -> new TagDto(
                        projection.tagId(),
                        projection.name(),
                        projection.slug()
                    ),
                    Collectors.toList()
                )
            ));

        List<PostSummaryDto> content = summaries.stream()
            .map(summary -> new PostSummaryDto(
                summary.postId(),
                summary.title(),
                summary.summary(),
                summary.thumbnailImageId(),
                summary.categoryName(),
                summary.categorySlug(),
                tagsByPostId.getOrDefault(
                    summary.postId(),
                    List.of()
                ),
                summary.createdAt(),
                summary.updatedAt()
            ))
            .toList();

        return new PageImpl<>(
            content,
            pageable,
            totalElements
        );
    }

    private long countPublishedPostsByKeyword(String keyword) {
        String sql = """
            SELECT COUNT(*)
            FROM posts p
            WHERE MATCH(p.published_title, p.published_content)
                AGAINST (:keyword IN NATURAL LANGUAGE MODE)
            """;

        Number result = (Number) entityManager
            .createNativeQuery(sql)
            .setParameter("keyword", keyword)
            .getSingleResult();

        return result.longValue();
    }

    private BooleanExpression categoryEq(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) return null;
        return category.slug.eq(categorySlug);
    }

    private BooleanExpression hasAllTags(List<String> tagSlugs) {
        if (tagSlugs == null || tagSlugs.isEmpty()) return null;

        return post.id.in(
            JPAExpressions
                .select(postTag.post.id)
                .from(postTag)
                .join(postTag.tag, tag)
                .where(tag.slug.in(tagSlugs))
                .groupBy(postTag.post.id)
                .having(tag.id.countDistinct().eq((long) tagSlugs.size()))
        );
    }
}
