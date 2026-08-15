package yooze.withme.domain.todo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.todo.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserUserIdAndDeletedAtIsNullOrderBySortOrderAsc(Long userId);

    @Query("select max(c.sortOrder) from Category c where c.user.userId = :userId and c.deletedAt is null")
    Optional<Long> findMaxSortOrder(@Param("userId") Long userId);

    boolean existsByUserUserIdAndCategoryNameAndDeletedAtIsNull(Long userId, String categoryName);

    boolean existsByUserUserIdAndCategoryNameAndCategoryIdNotAndDeletedAtIsNull(
            Long userId,
            String categoryName,
            Long categoryId
    );
}
