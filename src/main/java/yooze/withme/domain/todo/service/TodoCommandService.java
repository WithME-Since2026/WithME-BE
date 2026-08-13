package yooze.withme.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
}
