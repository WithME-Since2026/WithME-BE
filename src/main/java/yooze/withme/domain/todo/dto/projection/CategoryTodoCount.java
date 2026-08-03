package yooze.withme.domain.todo.dto.projection;

public record CategoryTodoCount(
        Long categoryId,
        long todoCount
) {
}
