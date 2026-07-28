package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Size(max = 64, message = "카테고리 이름은 64자를 초과할 수 없습니다.")
        String categoryName,

        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "카테고리 색상은 #RRGGBB 형식이어야 합니다."
        )
        String categoryColor
) {
}
