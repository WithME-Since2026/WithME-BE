package yooze.withme.domain.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.todo.dto.projection.CategoryTodoCount;
import yooze.withme.domain.todo.dto.response.CategoryDetailResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.repository.CategoryRepository;
import yooze.withme.domain.todo.repository.TodoRepository;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private CategoryQueryService categoryQueryService;

    @Test
    void getCategoriesCombinesTodoCountsAndDefaultsMissingCountToZero() {
        User user = User.builder()
                .userId(USER_ID)
                .nickname("사용자")
                .build();
        Category first = category(10L, user, "운동", 0L);
        Category second = category(11L, user, "공부", 1L);

        when(categoryRepository.findByUserUserIdAndDeletedAtIsNullOrderBySortOrderAsc(USER_ID))
                .thenReturn(List.of(first, second));
        when(todoRepository.countTodosByCategory(USER_ID))
                .thenReturn(List.of(new CategoryTodoCount(first.getCategoryId(), 5L)));

        List<CategoryDetailResponse> responses = categoryQueryService.getCategories(USER_ID);

        assertThat(responses)
                .extracting(
                        CategoryDetailResponse::categoryId,
                        CategoryDetailResponse::sortOrder,
                        CategoryDetailResponse::todoCount
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 0L, 5L),
                        org.assertj.core.groups.Tuple.tuple(11L, 1L, 0L)
                );
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
