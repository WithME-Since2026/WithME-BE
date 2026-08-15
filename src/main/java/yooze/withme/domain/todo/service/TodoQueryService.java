package yooze.withme.domain.todo.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.todo.dto.response.TodoResponse;
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
}
