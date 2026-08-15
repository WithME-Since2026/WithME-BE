package yooze.withme.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.todo.dto.request.CompleteTodoRequest;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
import yooze.withme.domain.todo.dto.request.DeleteTodoRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoDateRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoRequest;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.CategoryRepository;
import yooze.withme.domain.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class TodoCommandService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    /** todo 생성: 카테고리는 선택이며, 지정된 경우 본인 소유인지 검증한다 */
    public TodoResponse createTodo(Long userId, CreateTodoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        Category category = resolveCategory(userId, request.categoryId());

        Todo todo = todoRepository.save(Todo.builder()
                .user(user)
                .category(category)
                .title(request.title())
                .dueDate(request.dueDate())
                .notificationStatus(request.notificationStatus())
                .build());

        return TodoResponse.from(todo);
    }

    /** todo 수정(제목/카테고리/알림): null 인 필드는 기존 값을 유지하며, categoryId 지정 시 본인 소유인지 검증한다 */
    public TodoResponse updateTodo(Long userId, UpdateTodoRequest request) {
        Todo todo = findOwnedTodo(userId, request.todoId());

        if (request.categoryId() != null) {
            Category category = resolveCategory(userId, request.categoryId());
            todo.changeCategory(category);
        }
        todo.update(request.title(), null, request.notificationStatus());

        return TodoResponse.from(todo);
    }

    /** todo 마감일 수정 */
    public TodoResponse updateTodoDate(Long userId, UpdateTodoDateRequest request) {
        Todo todo = findOwnedTodo(userId, request.todoId());
        todo.update(null, request.dueDate(), null);
        return TodoResponse.from(todo);
    }

    /** todo 완료/미완료 처리 */
    public TodoResponse completeTodo(Long userId, CompleteTodoRequest request) {
        Todo todo = findOwnedTodo(userId, request.todoId());
        todo.updateCompleted(request.completed());
        return TodoResponse.from(todo);
    }

    /** todo 삭제: 소프트 삭제로 처리한다 */
    public void deleteTodo(Long userId, DeleteTodoRequest request) {
        Todo todo = findOwnedTodo(userId, request.todoId());
        todo.delete();
    }

    private Category resolveCategory(Long userId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));
        if (!category.getUser().getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.CATEGORY_FORBIDDEN);
        }
        return category;
    }

    /**
     * 본인 소유이면서 삭제되지 않은 todo 를 조회한다.
     * 삭제된 todo 와 남의 todo 는 모두 없는 것으로 취급한다 — 403으로 구분하면 todoId 존재 여부가 새어나간다.
     */
    private Todo findOwnedTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TODO_NOT_FOUND));
        if (todo.deleted() || !todo.getUser().getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.TODO_NOT_FOUND);
        }
        return todo;
    }
}
