package yooze.withme.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.response.PageResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.todo.controller.docs.TodoControllerDocs;
import yooze.withme.domain.todo.dto.request.CompleteTodoRequest;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
import yooze.withme.domain.todo.dto.request.DeleteTodoRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoDateRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoRequest;
import yooze.withme.domain.todo.dto.response.TodoResponse;
import yooze.withme.domain.todo.service.TodoCommandService;
import yooze.withme.domain.todo.service.TodoQueryService;

@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
public class TodoController implements TodoControllerDocs {

    private final TodoCommandService todoCommandService;
    private final TodoQueryService todoQueryService;

    // 다른 컨트롤러와 달리 userDetails.getUsername()을 파싱하지 않고 JwtAuthenticationFilter가
    // 심어둔 request attribute로 userId를 받는다. 인증 사용자 식별 방식이 전역 통일되면 함께 정리 예정.
    @Override
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateTodoRequest request
    ) {
        TodoResponse response = todoCommandService.createTodo(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_TODO_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<PageResponse<TodoResponse>>> getTodos(
            @RequestAttribute("userId") Long userId,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable
    ) {
        PageResponse<TodoResponse> response = todoQueryService.getTodos(userId, pageable);
        return ApiResponse.success(SuccessStatus.GET_TODOS_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateTodoRequest request
    ) {
        TodoResponse response = todoCommandService.updateTodo(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_TODO_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodoDate(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateTodoDateRequest request
    ) {
        TodoResponse response = todoCommandService.updateTodoDate(userId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_TODO_DATE_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<TodoResponse>> completeTodo(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CompleteTodoRequest request
    ) {
        TodoResponse response = todoCommandService.completeTodo(userId, request);
        return ApiResponse.success(SuccessStatus.COMPLETE_TODO_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteTodo(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody DeleteTodoRequest request
    ) {
        todoCommandService.deleteTodo(userId, request);
        return ApiResponse.success(SuccessStatus.DELETE_TODO_SUCCESS);
    }
}
