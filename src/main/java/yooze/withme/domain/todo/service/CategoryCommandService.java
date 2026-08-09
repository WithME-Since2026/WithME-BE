package yooze.withme.domain.todo.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.ConstraintViolations;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.todo.dto.request.CreateCategoryRequest;
import yooze.withme.domain.todo.dto.request.UpdateCategoryRequest;
import yooze.withme.domain.todo.dto.response.CategoryResponse;
import yooze.withme.domain.todo.entity.Category;
import yooze.withme.domain.todo.repository.CategoryRepository;
import yooze.withme.domain.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    public CategoryResponse createCategory(Long userId, CreateCategoryRequest request) {
        // sortOrder 계산은 "현재 목록을 읽고 → 다음 값을 정해서 → 저장"하는 읽기-수정-쓰기다.
        // 사용자 행을 먼저 잠가 같은 사용자의 동시 생성/재정렬이 같은 값을 읽지 못하게 한다.
        User user = lockUser(userId);

        // 선검사는 정상 케이스를 빠르게 걸러낼 뿐, 동시 요청에서는 양쪽 모두 통과 가능
        // 중복 여부의 최종 판정은 아래 flush 에서 발생하는 유니크 제약 위반으로 위임
        if (categoryRepository.existsByUserUserIdAndCategoryName(userId, request.categoryName())) {
            throw new GeneralException(ErrorStatus.DUPLICATE_CATEGORY_NAME);
        }

        long sortOrder = categoryRepository.findMaxSortOrder(userId)
                .map(order -> order + 1)
                .orElse(0L);
        String color = request.categoryColor() == null
                ? Category.DEFAULT_COLOR
                : request.categoryColor();

        Category category = Category.builder()
                .user(user)
                .categoryName(request.categoryName())
                .categoryColor(color)
                .sortOrder(sortOrder)
                .build();

        try {
            categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicateName(e);
        }

        return CategoryResponse.from(category);
    }

    public CategoryResponse updateCategory(Long userId, UpdateCategoryRequest request) {
        // 정렬 순서를 건드리는 요청만 잠근다.
        // 이름/색상만 바꾸는 요청은 목록 전체를 재계산하지 않으므로 직렬화할 이유가 없고,
        // 이름 중복의 최종 판정은 아래 flush 의 유니크 제약이 담당한다.
        // 목록을 읽기 전에 잠가야 하므로 카테고리 조회보다 먼저 획득한다.
        if (request.sortOrder() != null) {
            lockUser(userId);
        }

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

        try {
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicateName(e);
        }

        return CategoryResponse.from(category);
    }

    /** 카테고리 삭제: 이 카테고리를 쓰던 todo는 삭제되지 않고 카테고리만 해제된다 */
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CATEGORY_NOT_FOUND));

        if (!category.getUser().getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.CATEGORY_FORBIDDEN);
        }

        todoRepository.clearCategory(categoryId);
        categoryRepository.delete(category);
    }

    /**
     * 사용자별 정렬 순서 재계산을 직렬화하기 위한 게이트.
     * 트랜잭션이 끝날 때까지 유지되므로 읽기-수정-쓰기 전체가 보호된다.
     */
    private User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    /**
     * 카테고리 이름 유니크 제약 위반만 409 로 변환하고, 그 외 무결성 위반은 그대로 전파한다.
     */
    private GeneralException translateDuplicateName(DataIntegrityViolationException e) {
        if (ConstraintViolations.matches(e, Category.UK_CATEGORY_USER_NAME)) {
            return new GeneralException(ErrorStatus.DUPLICATE_CATEGORY_NAME);
        }
        throw e;
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
