package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateTodoDateRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId,

        @NotNull(message = "마감일은 필수입니다.")
        LocalDate dueDate
) {
}
