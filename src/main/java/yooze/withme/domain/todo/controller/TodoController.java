package yooze.withme.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.todo.controller.docs.TodoControllerDocs;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.service.TodoCommandService;

@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
public class TodoController implements TodoControllerDocs {

    private final TodoCommandService todoCommandService;

    @Override
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateTodoRequest request
    ) {
        TodoResponse response = todoCommandService.createTodo(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_TODO_SUCCESS, response);
    }
}
