package yooze.withme.domain.todo.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.todo.dto.request.CreateTodoRequest;
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
}
