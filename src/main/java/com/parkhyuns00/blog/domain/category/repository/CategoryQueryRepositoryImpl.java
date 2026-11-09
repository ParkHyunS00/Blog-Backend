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
public class CategoryQueryRepositoryImpl implements CategoryQueryRepository {

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

        QPost latestPost = new QPost("latestPost");

        List<Tuple> latestPosts = queryFactory
            .select(
                category.id,
                post.id,
                post.title,
                post.summary,
                post.createdAt
            )
            .from(category)
            .join(post).on(post.id.eq(
                JPAExpressions
                    .select(latestPost.id)
                    .from(latestPost)
                    .where(
                        latestPost.category.eq(category),
                        latestPost.status.eq(status)
                    )
                    .orderBy(
                        latestPost.category.id.desc(),
                        latestPost.status.desc(),
                        latestPost.createdAt.desc(),
                        latestPost.id.desc()
                    )
                    .limit(1)
            ))
            .orderBy(category.name.asc(), category.id.asc())
            .fetch();

        Map<Long, Tuple> latestPostByCategoryId = latestPosts.stream()
            .collect(Collectors.toMap(
                row -> row.get(category.id),
                Function.identity()
            ));

        return categories.stream()
            .map(row -> {
                Long categoryId = row.get(category.id);
                Tuple latestPostRow = latestPostByCategoryId.get(categoryId);

                return new CategoryWithLatestPostDto(
                    categoryId,
                    row.get(category.name),
                    row.get(category.slug),
                    row.get(post.id.count()),
                    new CategoryLatestPostDto(
                        latestPostRow.get(post.id),
                        latestPostRow.get(post.title),
                        latestPostRow.get(post.summary),
                        latestPostRow.get(post.createdAt)
                    )
                );
            })
            .toList();
    }
}
