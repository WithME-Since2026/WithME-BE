package yooze.withme.domain.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.CategoryRepository;
import yooze.withme.domain.todo.repository.TodoRepository;

@ExtendWith(MockitoExtension.class)
class TodoCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 10);

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TodoCommandService todoCommandService;

    @Test
    void createTodoWithoutCategorySavesUncompletedTodo() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(todoRepository.save(any(Todo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoCommandService.createTodo(
                USER_ID,
                new CreateTodoRequest("운동하기", DUE_DATE, null, true)
        );

        assertThat(response.title()).isEqualTo("운동하기");
        assertThat(response.dueDate()).isEqualTo(DUE_DATE);
        assertThat(response.categoryId()).isNull();
        assertThat(response.completed()).isFalse();
        assertThat(response.notificationStatus()).isTrue();
    }

    @Test
    void createTodoWithOwnedCategoryAttachesCategory() {
        Category category = category(10L, user());
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        when(todoRepository.save(any(Todo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TodoResponse response = todoCommandService.createTodo(
                USER_ID,
                new CreateTodoRequest("운동하기", DUE_DATE, category.getCategoryId(), false)
        );

        assertThat(response.categoryId()).isEqualTo(category.getCategoryId());
    }

    @Test
    void createTodoThrowsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoCommandService.createTodo(
                USER_ID,
                new CreateTodoRequest("운동하기", DUE_DATE, null, false)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }

    @Test
    void createTodoThrowsWhenCategoryNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoCommandService.createTodo(
                USER_ID,
                new CreateTodoRequest("운동하기", DUE_DATE, 99L, false)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.CATEGORY_NOT_FOUND);
    }

    @Test
    void createTodoThrowsWhenCategoryBelongsToAnotherUser() {
        User owner = User.builder().userId(2L).nickname("다른사용자").build();
        Category category = category(10L, owner);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> todoCommandService.createTodo(
                USER_ID,
                new CreateTodoRequest("운동하기", DUE_DATE, category.getCategoryId(), false)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.CATEGORY_FORBIDDEN);
    }

    private User user() {
        return User.builder()
                .userId(USER_ID)
                .nickname("사용자")
                .build();
    }

    private Category category(Long id, User owner) {
        return Category.builder()
                .categoryId(id)
                .user(owner)
                .categoryName("업무")
                .categoryColor(Category.DEFAULT_COLOR)
                .sortOrder(0L)
                .build();
    }
}
