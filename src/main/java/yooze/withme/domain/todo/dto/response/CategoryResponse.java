package yooze.withme.domain.todo.dto.response;

import yooze.withme.domain.todo.entity.Category;

public record CategoryResponse(
        Long categoryId,
        String categoryName,
        String categoryColor,
        Long sortOrder
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getCategoryColor(),
                category.getSortOrder()
        );
    }
}
