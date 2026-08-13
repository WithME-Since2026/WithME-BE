package yooze.withme.domain.todo.dto.response;

import java.time.LocalDate;
import yooze.withme.domain.todo.entity.Todo;

public record TodoResponse(
        Long todoId,
        Long categoryId,
        String title,
        LocalDate dueDate,
        boolean completed,
        boolean notificationStatus
) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getTodoId(),
                todo.getCategory() == null ? null : todo.getCategory().getCategoryId(),
                todo.getTitle(),
                todo.getDueDate(),
                todo.isCompleted(),
                todo.isNotificationStatus()
        );
    }
}
