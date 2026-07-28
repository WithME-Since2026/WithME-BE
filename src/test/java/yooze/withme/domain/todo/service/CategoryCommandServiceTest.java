package yooze.withme.domain.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.todo.dto.request.CreateCategoryRequest;
import yooze.withme.domain.todo.dto.request.UpdateCategoryRequest;
import yooze.withme.domain.todo.dto.response.CategoryResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryCommandService categoryCommandService;

    @Test
    void createCategoryUsesDefaultColorAndNextSortOrder() {
        User user = user();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByUserUserIdAndCategoryName(USER_ID, "운동"))
                .thenReturn(false);
        when(categoryRepository.findMaxSortOrder(USER_ID)).thenReturn(Optional.of(2L));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryCommandService.createCategory(
                USER_ID,
                new CreateCategoryRequest("운동", null)
        );

        assertThat(response.categoryName()).isEqualTo("운동");
        assertThat(response.categoryColor()).isEqualTo(Category.DEFAULT_COLOR);
        assertThat(response.sortOrder()).isEqualTo(3L);
    }

    @Test
    void updateCategoryMovesTargetAndRenumbersAllCategories() {
        User user = user();
        Category categoryA = category(10L, user, "A", 0L);
        Category categoryB = category(11L, user, "B", 1L);
        Category categoryC = category(12L, user, "C", 2L);
        Category categoryD = category(13L, user, "D", 3L);
        List<Category> categories = new ArrayList<>(
                List.of(categoryA, categoryB, categoryC, categoryD)
        );

        when(categoryRepository.findById(categoryC.getCategoryId()))
                .thenReturn(Optional.of(categoryC));
        when(categoryRepository.findByUserUserIdOrderBySortOrderAsc(USER_ID))
                .thenReturn(categories);

        CategoryResponse response = categoryCommandService.updateCategory(
                USER_ID,
                new UpdateCategoryRequest(categoryC.getCategoryId(), null, null, 1L)
        );

        assertThat(response.sortOrder()).isEqualTo(1L);
        assertThat(categoryA.getSortOrder()).isEqualTo(0L);
        assertThat(categoryC.getSortOrder()).isEqualTo(1L);
        assertThat(categoryB.getSortOrder()).isEqualTo(2L);
        assertThat(categoryD.getSortOrder()).isEqualTo(3L);
    }

    @Test
    void updateCategoryClampsSortOrderToLastPosition() {
        User user = user();
        Category categoryA = category(10L, user, "A", 0L);
        Category categoryB = category(11L, user, "B", 1L);
        Category categoryC = category(12L, user, "C", 2L);
        List<Category> categories = new ArrayList<>(
                List.of(categoryA, categoryB, categoryC)
        );

        when(categoryRepository.findById(categoryA.getCategoryId()))
                .thenReturn(Optional.of(categoryA));
        when(categoryRepository.findByUserUserIdOrderBySortOrderAsc(USER_ID))
                .thenReturn(categories);

        CategoryResponse response = categoryCommandService.updateCategory(
                USER_ID,
                new UpdateCategoryRequest(categoryA.getCategoryId(), null, null, 100L)
        );

        assertThat(response.sortOrder()).isEqualTo(2L);
        assertThat(categoryB.getSortOrder()).isEqualTo(0L);
        assertThat(categoryC.getSortOrder()).isEqualTo(1L);
        assertThat(categoryA.getSortOrder()).isEqualTo(2L);
    }

    private User user() {
        return User.builder()
                .userId(USER_ID)
                .nickname("사용자")
                .build();
    }

    private Category category(Long id, User user, String name, Long sortOrder) {
        return Category.builder()
                .categoryId(id)
                .user(user)
                .categoryName(name)
                .categoryColor(Category.DEFAULT_COLOR)
                .sortOrder(sortOrder)
                .build();
    }
}
