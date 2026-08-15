package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;

public record CompleteTodoRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId,

        @NotNull(message = "완료 여부는 필수입니다.")
        Boolean completed
) {
}
