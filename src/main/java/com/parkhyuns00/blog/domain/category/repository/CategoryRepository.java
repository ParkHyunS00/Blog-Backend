package com.parkhyuns00.blog.domain.category.repository;

import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.dto.CategoryWithPostCountDto;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    List<Category> findAllByOrderByNameAsc();

    @Query("""
        select new com.parkhyuns00.blog.domain.category.repository.dto.CategoryWithPostCountDto(
            c.id,
            c.name,
            c.slug,
            count(p.id)
        )
        from Category c
        left join Post p on p.category = c and p.status = :status
        group by c.id, c.name, c.slug
        order by c.name asc
    """)
    List<CategoryWithPostCountDto> findAllWithPostCount(PostStatus status);
}
