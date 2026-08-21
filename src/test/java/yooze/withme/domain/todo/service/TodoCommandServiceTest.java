package yooze.withme.domain.todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.service.RecurrenceCommandService;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
import yooze.withme.domain.todo.dto.request.DeleteTodoOccurrenceRequest;
import yooze.withme.domain.todo.dto.request.DeleteTodoRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoOccurrenceRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoRequest;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.CategoryRepository;
import yooze.withme.domain.todo.repository.TodoRepository;

@ExtendWith(MockitoExtension.class)
class TodoCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TODO_ID = 5L;
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 10);

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RecurrenceCommandService recurrenceCommandService;

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

    @Test
    void createTodoSavesRecurrenceWithDueDateAsAnchor() {
        RecurrenceRequest rule = new RecurrenceRequest(
                RecurrenceFreq.WEEKLY, 1, "MON", RecurrenceEndType.NEVER, null, null);
        RecurrenceResponse saved = new RecurrenceResponse(
                RecurrenceFreq.WEEKLY, 1, "MON", RecurrenceEndType.NEVER, null, null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(todoRepository.save(any(Todo.class))).thenReturn(todo(TODO_ID));
        when(recurrenceCommandService.upsert(
                RecurrenceOwnerType.TODO, TODO_ID, DUE_DATE, rule)).thenReturn(saved);

        TodoResponse response = todoCommandService.createTodo(
                USER_ID,
                new CreateTodoRequest("주간 회고", DUE_DATE, null, false, rule)
        );

        assertThat(response.recurrence()).isEqualTo(saved);
    }

    @Test
    void updateTodoWithRecurringFalseRemovesRule() {
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(todo(TODO_ID)));

        TodoResponse response = todoCommandService.updateTodo(
                USER_ID,
                new UpdateTodoRequest(TODO_ID, null, null, null, false, null)
        );

        assertThat(response.recurrence()).isNull();
        verify(recurrenceCommandService).delete(RecurrenceOwnerType.TODO, TODO_ID);
        verify(recurrenceCommandService, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void deleteTodoRemovesRecurrenceRule() {
        Todo todo = todo(TODO_ID);
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(todo));

        todoCommandService.deleteTodo(USER_ID, new DeleteTodoRequest(TODO_ID));

        assertThat(todo.deleted()).isTrue();
        verify(recurrenceCommandService).delete(RecurrenceOwnerType.TODO, TODO_ID);
    }

    @Test
    void updateOccurrenceOverridesCompletionForThatDateOnly() {
        Todo todo = todo(TODO_ID);
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(todo));
        when(recurrenceCommandService.override(any(), any(), any(), any(), any()))
                .thenReturn(new OccurrenceResponse(
                        DUE_DATE.plusWeeks(1), DUE_DATE.plusWeeks(1), null, null, null, true));

        OccurrenceResponse response = todoCommandService.updateOccurrence(
                USER_ID,
                new UpdateTodoOccurrenceRequest(
                        TODO_ID, DUE_DATE.plusWeeks(1), null, null, true)
        );

        assertThat(response.completed()).isTrue();
        // 원본의 completed 는 회차 단위 완료로 바뀌지 않는다
        assertThat(todo.isCompleted()).isFalse();
    }

    @Test
    void deleteOccurrenceSkipsOnlyThatOccurrence() {
        Todo todo = todo(TODO_ID);
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(todo));

        todoCommandService.deleteOccurrence(
                USER_ID,
                new DeleteTodoOccurrenceRequest(TODO_ID, DUE_DATE.plusWeeks(1))
        );

        assertThat(todo.deleted()).isFalse();
        verify(recurrenceCommandService).skip(
                RecurrenceOwnerType.TODO, TODO_ID, DUE_DATE, DUE_DATE.plusWeeks(1));
    }

    private Todo todo(Long todoId) {
        return Todo.builder()
                .todoId(todoId)
                .user(user())
                .title("주간 회고")
                .dueDate(DUE_DATE)
                .build();
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
