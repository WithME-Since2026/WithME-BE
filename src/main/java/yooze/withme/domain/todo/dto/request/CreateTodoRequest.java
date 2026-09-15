package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;

public record CreateTodoRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        String title,

        @NotNull(message = "마감일은 필수입니다.")
        @FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.")
        LocalDate dueDate,

        Long categoryId,

        boolean notificationStatus,

        @Valid
        RecurrenceRequest recurrence
) {

    /** 앞뒤 공백을 제거해 카테고리 이름과 동일한 방식으로 정규화 */
    public CreateTodoRequest {
        title = title == null ? null : title.strip();
    }

    public CreateTodoRequest(
            String title,
            LocalDate dueDate,
            Long categoryId,
            boolean notificationStatus
    ) {
        this(title, dueDate, categoryId, notificationStatus, null);
    }
}
