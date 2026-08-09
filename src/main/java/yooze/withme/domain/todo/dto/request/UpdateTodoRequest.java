package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTodoRequest(
        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        @FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.")
        LocalDate dueDate,

        Long categoryId,

        Boolean notificationStatus
) {

    /** 생성과 동일하게 앞뒤 공백을 제거한다. null 은 변경하지 않음을 뜻한다 */
    public UpdateTodoRequest {
        title = title == null ? null : title.strip();
    }
}
