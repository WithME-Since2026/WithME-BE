package yooze.withme.domain.todo.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.todo.dto.request.CreateCategoryRequest;
import yooze.withme.domain.todo.dto.request.UpdateCategoryRequest;
import yooze.withme.domain.todo.dto.response.CategoryResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.repository.CategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryResponse createCategory(Long userId, CreateCategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        if (categoryRepository.existsByUserUserIdAndCategoryName(userId, request.categoryName())) {
            throw new GeneralException(ErrorStatus.DUPLICATE_CATEGORY_NAME);
        }

        long sortOrder = categoryRepository.findMaxSortOrder(userId)
                .map(order -> order + 1)
                .orElse(0L);
        String color = request.categoryColor() == null
                ? Category.DEFAULT_COLOR
                : request.categoryColor();

        Category category = categoryRepository.save(Category.builder()
                .user(user)
                .categoryName(request.categoryName())
                .categoryColor(color)
                .sortOrder(sortOrder)
                .build());

        return CategoryResponse.from(category);
    }

    public CategoryResponse updateCategory(Long userId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));

        if (!category.getUser().getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.CATEGORY_FORBIDDEN);
        }

        if (request.categoryName() != null
                && !request.categoryName().equals(category.getCategoryName())
                && categoryRepository.existsByUserUserIdAndCategoryNameAndCategoryIdNot(
                        userId,
                        request.categoryName(),
                        category.getCategoryId()
                )) {
            throw new GeneralException(ErrorStatus.DUPLICATE_CATEGORY_NAME);
        }

        if (request.sortOrder() != null
                && !request.sortOrder().equals(category.getSortOrder())) {
            reorderCategories(userId, category, request.sortOrder());
        }

        category.update(request.categoryName(), request.categoryColor(), null);
        return CategoryResponse.from(category);
    }

    private void reorderCategories(Long userId, Category target, long requestedOrder) {
        List<Category> categories = categoryRepository.findByUserUserIdOrderBySortOrderAsc(userId);
        categories.removeIf(category -> category.getCategoryId().equals(target.getCategoryId()));

        int targetIndex = (int) Math.min(requestedOrder, categories.size());
        categories.add(targetIndex, target);

        for (int index = 0; index < categories.size(); index++) {
            categories.get(index).update(null, null, (long) index);
        }
    }
}
