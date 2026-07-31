package com.parkhyuns00.blog.domain.tag.repository;

import com.parkhyuns00.blog.domain.tag.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    boolean existsByName(String name);

    Optional<Tag> findBySlug(String slug);

    List<Tag> findAllByOrderByNameAsc();
}
