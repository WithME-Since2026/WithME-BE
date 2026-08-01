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
    /**
     * 앞뒤 공백을 제거해 "업무", "업무 ", " 업무" 가 같은 이름으로 취급되도록 한다.
     * 역직렬화 직후 실행되므로 아래 검증과 중복 검사 모두 정규화된 값을 기준으로 동작한다.
     */
    public CreateCategoryRequest {
        categoryName = strip(categoryName);
        categoryColor = strip(categoryColor);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
