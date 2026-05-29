package com.parkhyuns00.blog.domain.tag.service;

import com.parkhyuns00.blog.domain.tag.exception.TagException;
import com.parkhyuns00.blog.domain.tag.exception.TagExceptionCode;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.repository.TagRepository;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.parkhyuns00.blog.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private static final int MAX_TAG_COUNT = 5;

    private final TagRepository tagRepository;
    private final SlugUtil slugUtil;

    @Transactional
    public List<Tag> getOrCreateAllByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        if (names.size() > MAX_TAG_COUNT) {
            throw new TagException(TagExceptionCode.TOO_MANY_TAGS);
        }

        List<TagNameAndSlug> tagNameAndSlugs = names.stream()
            .map(name -> new TagNameAndSlug(name, generateSlug(name)))
            .toList();

        validateDuplicatedSlugs(tagNameAndSlugs);

        return tagNameAndSlugs.stream()
            .map(this::findOrCreate)
            .toList();
    }

    public List<TagDto> getTags() {
        return tagRepository.findAllByOrderByNameAsc().stream()
            .map(TagDto::from)
            .toList();
    }

    private void validateDuplicatedSlugs(List<TagNameAndSlug> tagNameAndSlugs) {
        long distinctSlugCount = tagNameAndSlugs.stream()
            .map(TagNameAndSlug::slug)
            .distinct()
            .count();

        if (tagNameAndSlugs.size() != distinctSlugCount) {
            throw new TagException(TagExceptionCode.TAG_NAME_DUPLICATED);
        }
    }

    private Tag findOrCreate(TagNameAndSlug tagNameAndSlug) {
        return tagRepository.findBySlug(tagNameAndSlug.slug())
            .orElseGet(() -> create(tagNameAndSlug.name(), tagNameAndSlug.slug()));
    }

    private Tag create(String name, String slug) {
        if (tagRepository.existsByName(name)) {
            throw new TagException(TagExceptionCode.TAG_NAME_DUPLICATED);
        }

        try {
            return tagRepository.saveAndFlush(new Tag(name, slug));
        } catch (DataIntegrityViolationException exception) {
            return tagRepository.findBySlug(slug)
                .orElseThrow(() -> new TagException(TagExceptionCode.TAG_SAVE_FAILED, exception));
        } catch (DataAccessException exception) {
            throw new TagException(TagExceptionCode.TAG_SAVE_FAILED, exception);
        }
    }

    private String generateSlug(String name) {
        try {
            return slugUtil.generate(name);
        } catch (IllegalArgumentException exception) {
            throw new TagException(TagExceptionCode.INVALID_TAG_NAME, exception);
        }
    }

    private record TagNameAndSlug(String name, String slug) { }
}
