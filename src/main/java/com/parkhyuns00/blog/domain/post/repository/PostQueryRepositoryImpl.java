package com.parkhyuns00.blog.domain.post.repository;

import static com.parkhyuns00.blog.domain.category.model.QCategory.category;
import static com.parkhyuns00.blog.domain.post.model.QPost.post;
import static com.parkhyuns00.blog.domain.post.model.QPostImage.postImage;
import static com.parkhyuns00.blog.domain.post.model.QPostTag.postTag;
import static com.parkhyuns00.blog.domain.tag.model.QTag.tag;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.dto.PostDetailProjection;
import com.parkhyuns00.blog.domain.post.repository.dto.PostDraftSummaryProjection;
import com.parkhyuns00.blog.domain.post.repository.dto.PostSummaryProjection;
import com.parkhyuns00.blog.domain.post.repository.dto.PostTagProjection;
import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostDraftSummaryDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

        List<PostSummaryProjection> summaries = queryFactory
            .select(Projections.constructor(
                PostSummaryProjection.class,
                post.id,
                post.title,
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

        Long total = queryFactory
            .select(post.count())
            .from(post)
            .join(post.category, category)
            .where(
                post.status.eq(PostStatus.PUBLISHED),
                categoryEq(condition.categorySlug()),
                hasAllTags(condition.tagSlugs())
            )
            .fetchOne();

        long totalElements = total == null ? 0 : total;

        return createSummaryPage(summaries, pageable, totalElements);
    }

    @Override
    public Optional<PostDetailDto> findPublishedPostById(Long postId) {
        PostDetailProjection detail = queryFactory
            .select(Projections.constructor(
                PostDetailProjection.class,
                post.id,
                post.title,
                post.summary,
                post.content,
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
            .where(postTag.post.id.eq(postId))
            .orderBy(tag.name.asc(), tag.id.asc())
            .fetch();

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
            select p.id
            from posts p
            where p.status = 'PUBLISHED'
                and (
                    match(p.title)
                    against (:keyword in natural language mode) > 0
                    or
                    match(p.content)
                    against (:keyword in natural language mode) > 0
                )
                order by (
                    match(p.title)
                    against (:keyword in natural language mode) * 2
                    +
                    match(p.content)
                    against (:keyword in natural language mode)
                ) desc,
            p.created_at desc,
            p.id desc
            """;

        List<?> rawIds = entityManager
            .createNativeQuery(sql)
            .setParameter("keyword", keyword)
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize())
            .getResultList();

        List<Long> postIds = rawIds.stream()
            .map(Number.class::cast)
            .map(Number::longValue)
            .toList();

        long totalElements = countPublishedPostsByKeyword(keyword);

        if (postIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, totalElements);
        }

        List<PostSummaryProjection> fetchedSummaries = findSummariesByPostIds(postIds);

        Map<Long, PostSummaryProjection> summaryByPostId = fetchedSummaries.stream()
            .collect(Collectors.toMap(
                PostSummaryProjection::postId,
                summary -> summary
            ));

        List<PostSummaryProjection> orderedSummaries = postIds.stream()
            .map(summaryByPostId::get)
            .filter(Objects::nonNull)
            .toList();

        return createSummaryPage(orderedSummaries, pageable, totalElements);
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
            select count(*)
            from posts p
            where p.status = 'PUBLISHED'
                and (
                    match(p.title)
                    against (:keyword in natural language mode) > 0
                    or
                    match(p.content)
                    against (:keyword in natural language mode) > 0
                )
            """;

        Number result = (Number) entityManager
            .createNativeQuery(sql)
            .setParameter("keyword", keyword)
            .getSingleResult();

        return result.longValue();
    }

    private List<PostSummaryProjection> findSummariesByPostIds(List<Long> ids) {
        return queryFactory
            .select(Projections.constructor(
                PostSummaryProjection.class,
                post.id,
                post.title,
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
            .where(post.id.in(ids))
            .fetch();
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
