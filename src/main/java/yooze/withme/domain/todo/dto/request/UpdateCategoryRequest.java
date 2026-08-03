package yooze.withme.domain.todo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @Size(min = 1, max = 64, message = "카테고리 이름은 1자 이상 64자 이하여야 합니다.")
        @Pattern(regexp = ".*\\S.*", message = "카테고리 이름은 공백일 수 없습니다.")
        String categoryName,

        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "카테고리 색상은 #RRGGBB 형식이어야 합니다."
        )
        String categoryColor,

        @PositiveOrZero(message = "정렬 순서는 0 이상이어야 합니다.")
        Long sortOrder
) {
    /**
     * 생성과 동일하게 앞뒤 공백을 제거한다. null 은 "변경하지 않음" 을 뜻하므로 그대로 둔다.
     */
    public UpdateCategoryRequest {
        categoryName = strip(categoryName);
        categoryColor = strip(categoryColor);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
