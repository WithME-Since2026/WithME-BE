package yooze.withme.domain.todo.service;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.response.PageResponse;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.service.RecurrenceQueryService;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoQueryService {

    private final TodoRepository todoRepository;
    private final RecurrenceQueryService recurrenceQueryService;

    /** 내 todo 목록을 페이지 단위로 조회한다. 반복 규칙은 페이지당 쿼리 1번으로 함께 붙인다 */
    public PageResponse<TodoResponse> getTodos(Long userId, Pageable pageable) {
        Page<Todo> todos = todoRepository.findByUserUserIdAndDeletedAtIsNull(userId, pageable);
        List<Long> todoIds = todos.map(Todo::getTodoId).getContent();
        Map<Long, RecurrenceResponse> recurrences =
                recurrenceQueryService.findAll(RecurrenceOwnerType.TODO, todoIds);

        return PageResponse.from(todos.map(todo ->
                TodoResponse.from(todo, recurrences.get(todo.getTodoId()))));
    }
}
