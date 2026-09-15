package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DeleteTodoOccurrenceRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId,

        @NotNull(message = "회차 날짜는 필수입니다.")
        LocalDate occurrenceDate
) {
}
