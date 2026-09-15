package yooze.withme.domain.todo.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;

public record UpdateTodoRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId,

        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        Long categoryId,

        Boolean notificationStatus,

        /** null 은 반복 설정 유지, false 는 반복 해제, true 는 recurrence 규칙 적용 */
        Boolean recurring,

        @Valid
        RecurrenceRequest recurrence
) {

    /** 생성과 동일하게 앞뒤 공백을 제거한다. null 은 변경하지 않음을 뜻한다 */
    public UpdateTodoRequest {
        title = title == null ? null : title.strip();
    }

    public UpdateTodoRequest(
            Long todoId,
            String title,
            Long categoryId,
            Boolean notificationStatus
    ) {
        this(todoId, title, categoryId, notificationStatus, null, null);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "recurring이 true이면 반복 규칙이 필요하고, false이면 반복 규칙을 보낼 수 없습니다.")
    public boolean isRecurrenceChangeValid() {
        if (recurring == null) {
            return recurrence == null;
        }
        return recurring == (recurrence != null);
    }
}
