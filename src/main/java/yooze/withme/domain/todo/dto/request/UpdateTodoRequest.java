package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTodoRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId,

        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        Long categoryId,

        Boolean notificationStatus
) {

    /** 생성과 동일하게 앞뒤 공백을 제거한다. null 은 변경하지 않음을 뜻한다 */
    public UpdateTodoRequest {
        title = title == null ? null : title.strip();
    }
}
