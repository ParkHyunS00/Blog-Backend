package com.parkhyuns00.blog.domain.post.repository;

import static com.parkhyuns00.blog.domain.category.model.QCategory.category;
import static com.parkhyuns00.blog.domain.post.model.QPost.post;
import static com.parkhyuns00.blog.domain.post.model.QPostImage.postImage;
import static com.parkhyuns00.blog.domain.post.model.QPostTag.postTag;
import static com.parkhyuns00.blog.domain.tag.model.QTag.tag;

import com.parkhyuns00.blog.domain.post.model.PostImageType;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.repository.dto.PostDetailProjection;
import com.parkhyuns00.blog.domain.post.repository.dto.PostSummaryProjection;
import com.parkhyuns00.blog.domain.post.repository.dto.PostTagProjection;
import com.parkhyuns00.blog.domain.post.service.dto.PostDetailDto;
import com.parkhyuns00.blog.domain.post.service.dto.PostSearchCondition;
import com.parkhyuns00.blog.domain.post.service.dto.PostSummaryDto;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostSummaryDto> findPublishedPosts(PostSearchCondition condition, Pageable pageable) {
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


        return new PageImpl<>(content, pageable, totalElements);
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
