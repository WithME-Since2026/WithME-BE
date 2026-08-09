package yooze.withme.domain.todo.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoQueryService {

    private final TodoRepository todoRepository;

    /** 내 todo 목록을 마감일 오름차순으로 조회한다 */
    public List<TodoResponse> getTodos(Long userId) {
        return todoRepository.findByUserIdAndNotDeleted(userId).stream()
                .map(TodoResponse::from)
                .toList();
    }

    /** todo 상세 조회: 본인 소유가 아니거나 삭제된 todo는 조회할 수 없다 */
    public TodoResponse getTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TODO_NOT_FOUND));
        if (todo.deleted()) {
            throw new GeneralException(ErrorStatus.TODO_NOT_FOUND);
        }
        if (!todo.getUser().getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.TODO_FORBIDDEN);
        }
        return TodoResponse.from(todo);
    }
}
