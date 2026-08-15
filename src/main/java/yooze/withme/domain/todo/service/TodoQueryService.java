package yooze.withme.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.response.PageResponse;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoQueryService {

    private final TodoRepository todoRepository;

    /** 내 todo 목록을 페이지 단위로 조회한다 */
    public PageResponse<TodoResponse> getTodos(Long userId, Pageable pageable) {
        Page<TodoResponse> page = todoRepository.findByUserUserIdAndDeletedAtIsNull(userId, pageable)
                .map(TodoResponse::from);
        return PageResponse.from(page);
    }
}
