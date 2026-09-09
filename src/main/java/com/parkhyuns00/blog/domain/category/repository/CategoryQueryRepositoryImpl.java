package com.parkhyuns00.blog.domain.category.repository;

import static com.parkhyuns00.blog.domain.category.model.QCategory.category;
import static com.parkhyuns00.blog.domain.post.model.QPost.post;

import com.parkhyuns00.blog.domain.category.service.dto.CategoryLatestPostDto;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryWithLatestPostDto;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.domain.post.model.QPost;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CategoryQueryRepositoryImpl implements CategoryQueryRepository{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CategoryWithLatestPostDto> findAllWithLatestPost(PostStatus status) {
        List<Tuple> categories = queryFactory
            .select(
                category.id,
                category.name,
                category.slug,
                post.id.count()
            )
            .from(category)
            .join(post).on(
                post.category.eq(category),
                post.status.eq(status)
            )
            .groupBy(category.id, category.name, category.slug)
            .orderBy(category.name.asc(), category.id.asc())
            .fetch();

        if (categories.isEmpty()) return List.of();

        QPost newerPost = new QPost("newerPost");

        List<Tuple> latestPosts = queryFactory
            .select(
                post.category.id,
                post.id,
                post.title,
                post.summary,
                post.createdAt
            )
            .from(post)
            .where(
                post.status.eq(status),
                JPAExpressions
                    .selectOne()
                    .from(newerPost)
                    .where(
                        newerPost.category.eq(post.category),
                        newerPost.status.eq(status),
                        newerPost.createdAt.gt(post.createdAt)
                            .or(newerPost.createdAt.eq(post.createdAt).and(newerPost.id.gt(post.id)))
                    )
                    .notExists()
            )
            .fetch();

        Map<Long, Tuple> latestPostByCategoryId = latestPosts.stream()
            .collect(Collectors.toMap(
                row -> row.get(post.category.id),
                Function.identity()
            ));

        return categories.stream()
            .map(row -> {
                Long categoryId = row.get(category.id);
                Tuple latestPost = latestPostByCategoryId.get(categoryId);

                return new CategoryWithLatestPostDto(
                    categoryId,
                    row.get(category.name),
                    row.get(category.slug),
                    row.get(post.id.count()),
                    new CategoryLatestPostDto(
                        latestPost.get(post.id),
                        latestPost.get(post.title),
                        latestPost.get(post.summary),
                        latestPost.get(post.createdAt)
                    )
                );
            })
            .toList();
    }
}
