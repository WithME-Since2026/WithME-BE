package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeleteTodoRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId
) {
}
