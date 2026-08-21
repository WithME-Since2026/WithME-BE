package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeleteCategoryRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId
) {
}
