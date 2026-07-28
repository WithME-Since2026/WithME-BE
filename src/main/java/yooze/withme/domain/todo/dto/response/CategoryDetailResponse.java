package yooze.withme.domain.todo.dto.response;

import yooze.withme.domain.todo.entity.Category;

public record CategoryDetailResponse(
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long sortOrder,
        long todoCount
) {

    public static CategoryDetailResponse of(Category category, long todoCount) {
        return new CategoryDetailResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryColor(),
                category.getSortOrder(),
                todoCount
        );
    }
}
