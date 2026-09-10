package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 반복 todo 의 특정 회차만 덮어쓰는 요청. 전달된 필드만 원본 값을 대체한다. */
public record UpdateTodoOccurrenceRequest(
        @NotNull(message = "todo ID는 필수입니다.")
        Long todoId,

        @NotNull(message = "회차 날짜는 필수입니다.")
        LocalDate occurrenceDate,

        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "제목은 공백일 수 없습니다.")
        String title,

        LocalDate date,

        Boolean completed
) {

    public UpdateTodoOccurrenceRequest {
        title = title == null ? null : title.strip();
    }
}
