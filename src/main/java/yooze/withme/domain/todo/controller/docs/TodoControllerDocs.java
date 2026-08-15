package yooze.withme.domain.todo.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.todo.dto.request.CompleteTodoRequest;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
import yooze.withme.domain.todo.dto.request.DeleteTodoRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoDateRequest;
import yooze.withme.domain.todo.dto.request.UpdateTodoRequest;
import yooze.withme.domain.todo.dto.response.TodoResponse;

@Tag(name = "Todo", description = "Todo 생성/조회/수정/삭제 API")
@SecurityRequirement(name = "bearerAuth")
public interface TodoControllerDocs {

    @Operation(summary = "todo 생성", description = "카테고리는 선택이며, 지정 시 본인 소유 카테고리여야 한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "todo 생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 카테고리")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 카테고리")
    @PostMapping
    ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateTodoRequest request
    );

    @Operation(summary = "todo 목록 조회", description = "삭제되지 않은 본인 todo를 마감일 오름차순으로 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "todo 목록 조회 성공")
    @GetMapping("/list")
    ResponseEntity<ApiResponse<List<TodoResponse>>> getTodos(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId
    );

    @Operation(summary = "todo 수정", description = "제목/카테고리/알림 여부를 수정한다. null인 필드는 기존 값을 유지한다. categoryId 지정 시 본인 소유 카테고리여야 한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "todo 수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 카테고리")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 본인 소유가 아닌 todo, 또는 존재하지 않는 카테고리"
    )
    @PatchMapping
    ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateTodoRequest request
    );

    @Operation(summary = "todo 날짜 수정")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "todo 날짜 수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 소유가 아닌 todo")
    @PatchMapping("/date")
    ResponseEntity<ApiResponse<TodoResponse>> updateTodoDate(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateTodoDateRequest request
    );

    @Operation(summary = "todo 완료 처리")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "todo 완료 처리 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 소유가 아닌 todo")
    @PatchMapping("/completion")
    ResponseEntity<ApiResponse<TodoResponse>> completeTodo(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CompleteTodoRequest request
    );

    @Operation(summary = "todo 삭제", description = "소프트 삭제로 처리된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "todo 삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 소유가 아닌 todo")
    @DeleteMapping
    ResponseEntity<ApiResponse<Void>> deleteTodo(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody DeleteTodoRequest request
    );
}
