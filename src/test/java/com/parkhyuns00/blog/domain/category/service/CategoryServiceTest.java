package com.parkhyuns00.blog.domain.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.domain.category.exception.CategoryException;
import com.parkhyuns00.blog.domain.category.exception.CategoryExceptionCode;
import com.parkhyuns00.blog.domain.category.model.Category;
import com.parkhyuns00.blog.domain.category.repository.CategoryRepository;
import com.parkhyuns00.blog.domain.category.repository.dto.CategoryWithPostCountDto;
import com.parkhyuns00.blog.domain.category.service.dto.CategoryDto;
import com.parkhyuns00.blog.domain.post.model.PostStatus;
import com.parkhyuns00.blog.util.SlugUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SlugUtil slugUtil;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("카테고리 목록을 이름 오름차순으로 조회하고 DTO 로 변환한다.")
    void test_get_categories_success() {
        Category java = new Category("Java", "java");
        Category spring = new Category("Spring", "spring");
        ReflectionTestUtils.setField(java, "id", 1L);
        ReflectionTestUtils.setField(spring, "id", 2L);

        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(java, spring));

        List<CategoryDto> result = categoryService.getCategories();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().categoryId()).isEqualTo(1L);
        assertThat(result.getFirst().name()).isEqualTo("Java");
        assertThat(result.getFirst().slug()).isEqualTo("java");
        assertThat(result.getLast().categoryId()).isEqualTo(2L);
        assertThat(result.getLast().name()).isEqualTo("Spring");
        assertThat(result.getLast().slug()).isEqualTo("spring");

        verify(categoryRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("게시글 수를 포함한 카테고리 목록을 발행 상태 기준으로 조회한다.")
    void test_get_categories_with_post_count_success() {
        List<CategoryWithPostCountDto> categories = List.of(
            new CategoryWithPostCountDto(1L, "Java", "java", 2L),
            new CategoryWithPostCountDto(2L, "Spring", "spring", 3L)
        );

        when(categoryRepository.findAllWithPostCount(PostStatus.PUBLISHED)).thenReturn(categories);

        List<CategoryWithPostCountDto> result = categoryService.getCategoriesWithPostCount();

        assertThat(result).isEqualTo(categories);

        verify(categoryRepository).findAllWithPostCount(PostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("slug 에 해당하는 카테고리가 있으면 기존 카테고리를 반환한다.")
    void test_get_or_create_by_name_return_existing_category() {
        Category category = new Category("Spring", "spring");

        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.of(category));

        Category result = categoryService.getOrCreateByName("Spring");

        assertThat(result).isSameAs(category);

        verify(slugUtil).generate("Spring");
        verify(categoryRepository).findBySlug("spring");
        verify(categoryRepository, never()).existsByName(anyString());
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    @DisplayName("slug 에 해당하는 카테고리가 없으면 새 카테고리를 저장하고 반환한다.")
    void test_get_or_create_by_name_create_category() {
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.empty());
        when(categoryRepository.existsByName("Spring")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.getOrCreateByName("Spring");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(result.getSlug()).isEqualTo("spring");

        verify(slugUtil).generate("Spring");
        verify(categoryRepository).findBySlug("spring");
        verify(categoryRepository).existsByName("Spring");
        verify(categoryRepository).saveAndFlush(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 이름 앞 뒤 공백은 제거해서 처리한다.")
    void test_get_or_create_by_name_trim() {
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.empty());
        when(categoryRepository.existsByName("Spring")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.getOrCreateByName("  Spring  ");

        assertThat(result.getName()).isEqualTo("Spring");
        assertThat(result.getSlug()).isEqualTo("spring");

        verify(slugUtil).generate("Spring");
        verify(categoryRepository).existsByName("Spring");
    }

    @Test
    @DisplayName("카테고리 이름이 null 이면 예외가 발생한다.")
    void test_get_or_create_by_name_fail_when_null() {
        assertThatThrownBy(() -> categoryService.getOrCreateByName(null))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);

        verifyNoInteractions(slugUtil);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("카테고리 이름이 공백이면 예외가 발생한다.")
    void test_get_or_create_by_name_fail_when_blank() {
        assertThatThrownBy(() -> categoryService.getOrCreateByName("   "))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.INVALID_CATEGORY_NAME);

        verifyNoInteractions(slugUtil);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("slug는 없지만 같은 이름의 카테고리가 있으면 예외가 발생한다.")
    void test_get_or_create_by_name_fail_when_name_duplicated() {
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.empty());
        when(categoryRepository.existsByName("Spring")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.getOrCreateByName("Spring"))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.CATEGORY_NAME_DUPLICATED);

        verify(categoryRepository).findBySlug("spring");
        verify(categoryRepository).existsByName("Spring");
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 저장 중 unique 충돌이 발생해도 slug로 재조회되면 기존 카테고리를 반환한다.")
    void test_get_or_create_by_name_return_existing_category_when_unique_conflict() {
        Category existingCategory = new Category("Spring", "spring");

        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.empty()).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.existsByName("Spring")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        Category result = categoryService.getOrCreateByName("Spring");

        assertThat(result).isSameAs(existingCategory);

        verify(categoryRepository, times(2)).findBySlug("spring");
        verify(categoryRepository).saveAndFlush(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 저장 중 unique 충돌이 발생했지만 slug로 재조회되지 않으면 저장 실패 예외가 발생한다.")
    void test_get_or_create_by_name_fail_when_unique_conflict_but_category_not_found() {
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.empty()).thenReturn(Optional.empty());
        when(categoryRepository.existsByName("Spring")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> categoryService.getOrCreateByName("Spring"))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.CATEGORY_SAVE_FAILED);

        verify(categoryRepository, times(2)).findBySlug("spring");
    }

    @Test
    @DisplayName("카테고리 저장 중 DB 오류가 발생하면 저장 실패 예외가 발생한다.")
    void test_get_or_create_by_name_fail_when_data_access_exception() {
        when(slugUtil.generate("Spring")).thenReturn("spring");
        when(categoryRepository.findBySlug("spring")).thenReturn(Optional.empty());
        when(categoryRepository.existsByName("Spring")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> categoryService.getOrCreateByName("Spring"))
            .isInstanceOf(CategoryException.class)
            .extracting("exceptionCode")
            .isEqualTo(CategoryExceptionCode.CATEGORY_SAVE_FAILED);

        verify(categoryRepository).saveAndFlush(any(Category.class));
        verify(categoryRepository).findBySlug("spring");
    }
}
