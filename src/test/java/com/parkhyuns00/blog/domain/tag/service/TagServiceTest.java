package com.parkhyuns00.blog.domain.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.tag.exception.TagException;
import com.parkhyuns00.blog.domain.tag.exception.TagExceptionCode;
import com.parkhyuns00.blog.domain.tag.model.Tag;
import com.parkhyuns00.blog.domain.tag.repository.TagRepository;
import com.parkhyuns00.blog.domain.tag.service.dto.TagDto;
import com.parkhyuns00.blog.util.SlugUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private SlugUtil slugUtil;

    @InjectMocks
    private TagService tagService;

    @Test
    @DisplayName("태그 목록을 이름 오름차순으로 조회하고 DTO 로 변환한다.")
    void test_get_tags_success() {
        Tag java = new Tag("Java", "java");
        Tag spring = new Tag("Spring", "spring");
        ReflectionTestUtils.setField(java, "id", 1L);
        ReflectionTestUtils.setField(spring, "id", 2L);

        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(java, spring));

        List<TagDto> result = tagService.getTags();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().tagId()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Java");
        assertThat(result.getFirst().slug()).isEqualTo("java");
        assertThat(result.getLast().tagId()).isEqualTo(2L);
        assertThat(result.getLast().name()).isEqualTo("Spring");
        assertThat(result.getLast().slug()).isEqualTo("spring");

        verify(tagRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("태그 이름 목록이 null 이면 빈 리스트를 반환한다.")
    void test_get_or_create_all_by_names_return_empty_list() {
        List<Tag> result = tagService.getOrCreateAllByNames(null);

        assertThat(result).isEmpty();

        verifyNoInteractions(slugUtil);
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("태그 이름 목록이 비어 있으면 빈 리스트를 반환한다.")
    void test_get_or_create_all_by_names_return_empty_list_when_name_empty() {
        List<Tag> result = tagService.getOrCreateAllByNames(List.of());

        assertThat(result).isEmpty();

        verifyNoInteractions(slugUtil);
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("slug 에 해당하는 태그가 있으면 기존 태그를 반환한다.")
    void test_get_or_create_all_by_names_return_existing_tag() {
        Tag tag = new Tag("Java", "java");

        when(slugUtil.generate("Java")).thenReturn("java");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.of(tag));

        List<Tag> result = tagService.getOrCreateAllByNames(List.of("Java"));

        assertThat(result).containsExactly(tag);

        verify(slugUtil).generate("Java");
        verify(tagRepository).findBySlug("java");
        verify(tagRepository, never()).existsByName(anyString());
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
    }

    @Test
    @DisplayName("slug 에 해당하는 태그가 없으면 새 태그를 저장하고 반환한다.")
    void test_get_or_create_all_by_names_create_tag() {
        when(slugUtil.generate("Java")).thenReturn("java");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.empty());
        when(tagRepository.existsByName("Java")).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Tag> result = tagService.getOrCreateAllByNames(List.of("Java"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Java");
        assertThat(result.getFirst().getSlug()).isEqualTo("java");

        verify(slugUtil).generate("Java");
        verify(tagRepository).findBySlug("java");
        verify(tagRepository).existsByName("Java");
        verify(tagRepository).saveAndFlush(any(Tag.class));
    }

    @Test
    @DisplayName("여러 태그를 입력하면 입력 순서대로 태그를 반환한다.")
    void test_get_or_create_all_by_names_return_tags_input_order() {
        Tag java = new Tag("Java", "java");
        Tag spring = new Tag("Spring", "spring");

        when(slugUtil.generate("Java")).thenReturn("java");
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.of(java));
        when(tagRepository.findBySlug("spring")).thenReturn(Optional.of(spring));

        List<Tag> result = tagService.getOrCreateAllByNames(List.of("Java", "Spring"));

        assertThat(result).containsExactly(java, spring);

        verify(slugUtil).generate("Java");
        verify(slugUtil).generate("Spring");
        verify(tagRepository).findBySlug("java");
        verify(tagRepository).findBySlug("spring");
    }

    @Test
    @DisplayName("각 태그 이름의 slug 는 한 번씩만 생성된다.")
    void test_get_or_create_all_by_names_generate_slug_once_per_name() {
        Tag java = new Tag("Java", "java");
        Tag spring = new Tag("Spring", "spring");

        when(slugUtil.generate("Java")).thenReturn("java");
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.of(java));
        when(tagRepository.findBySlug("spring")).thenReturn(Optional.of(spring));

        tagService.getOrCreateAllByNames(List.of("Java", "Spring"));

        verify(slugUtil, times(1)).generate("Java");
        verify(slugUtil, times(1)).generate("Spring");
    }

    @Test
    @DisplayName("태그가 5개를 초과하면 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_many_tags() {
        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(List.of(
            "tag1", "tag2", "tag3", "tag4", "tag5", "tag6"
        )))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.TOO_MANY_TAGS);

        verifyNoInteractions(slugUtil);
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("slug 가 중복되는 태그가 존재하면 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_slug_duplicated() {
        when(slugUtil.generate("Java")).thenReturn("java");
        when(slugUtil.generate("java")).thenReturn("java");

        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(List.of("java", "Java")))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.TAG_NAME_DUPLICATED);

        verify(slugUtil).generate("Java");
        verify(slugUtil).generate("java");
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("태그 이름이 null 이면 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_name_null() {
        when(slugUtil.generate(null)).thenThrow(new IllegalArgumentException("fail"));

        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(Arrays.asList((String)null)))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);

        verify(slugUtil).generate(null);
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("태그 이름이 공백이면 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_name_blank() {
        when(slugUtil.generate("   ")).thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(List.of("   ")))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.INVALID_TAG_NAME);

        verify(slugUtil).generate("   ");
        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("slug는 없지만 같은 이름의 태그가 있으면 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_name_duplicated() {
        when(slugUtil.generate("Java")).thenReturn("java");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.empty());
        when(tagRepository.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(List.of("Java")))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.TAG_NAME_DUPLICATED);

        verify(tagRepository).findBySlug("java");
        verify(tagRepository).existsByName("Java");
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
    }

    @Test
    @DisplayName("태그 저장 중 unique 충돌이 발생해도 slug로 재조회되면 기존 태그를 반환한다.")
    void test_get_or_create_all_by_names_return_existing_tag_when_unique_conflict() {
        Tag existingTag = new Tag("Java", "java");

        when(slugUtil.generate("Java")).thenReturn("java");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.empty()).thenReturn(Optional.of(existingTag));
        when(tagRepository.existsByName("Java")).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        List<Tag> result = tagService.getOrCreateAllByNames(List.of("Java"));

        assertThat(result).containsExactly(existingTag);

        verify(tagRepository, times(2)).findBySlug("java");
        verify(tagRepository).saveAndFlush(any(Tag.class));
    }

    @Test
    @DisplayName("태그 저장 중 unique 충돌이 발생했지만 slug로 재조회되지 않으면 저장 실패 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_unique_conflict_but_tag_not_found() {
        when(slugUtil.generate("Java")).thenReturn("java");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.empty()).thenReturn(Optional.empty());
        when(tagRepository.existsByName("Java")).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(List.of("Java")))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.TAG_SAVE_FAILED);

        verify(tagRepository, times(2)).findBySlug("java");
    }

    @Test
    @DisplayName("태그 저장 중 DB 오류가 발생하면 저장 실패 예외가 발생한다.")
    void test_get_or_create_all_by_names_fail_when_data_access_exception() {
        when(slugUtil.generate("Java")).thenReturn("java");
        when(tagRepository.findBySlug("java")).thenReturn(Optional.empty());
        when(tagRepository.existsByName("Java")).thenReturn(false);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> tagService.getOrCreateAllByNames(List.of("Java")))
            .isInstanceOf(TagException.class)
            .extracting("exceptionCode")
            .isEqualTo(TagExceptionCode.TAG_SAVE_FAILED);

        verify(tagRepository).saveAndFlush(any(Tag.class));
        verify(tagRepository).findBySlug("java");
    }
}
