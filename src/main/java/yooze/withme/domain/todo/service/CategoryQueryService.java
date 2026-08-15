package yooze.withme.domain.todo.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.todo.dto.projection.CategoryTodoCount;
import yooze.withme.domain.todo.dto.response.CategoryDetailResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.repository.CategoryRepository;
import yooze.withme.domain.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

    private final CategoryRepository categoryRepository;
    private final TodoRepository todoRepository;

    public List<CategoryDetailResponse> getCategories(Long userId) {
        List<Category> categories =
                categoryRepository.findByUserUserIdAndDeletedAtIsNullOrderBySortOrderAsc(userId);
        Map<Long, Long> todoCounts = todoRepository.countTodosByCategory(userId).stream()
                .collect(Collectors.toMap(
                        CategoryTodoCount::categoryId,
                        CategoryTodoCount::todoCount
                ));

        return categories.stream()
                .map(category -> CategoryDetailResponse.of(
                        category,
                        todoCounts.getOrDefault(category.getCategoryId(), 0L)
                ))
                .toList();
    }
}
