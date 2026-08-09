package yooze.withme.domain.todo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.todo.dto.projection.CategoryTodoCount;
import yooze.withme.domain.todo.entity.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("""
            select new yooze.withme.domain.todo.dto.projection.CategoryTodoCount(c.categoryId, count(t))
            from Category c
            left join Todo t on t.category = c and t.deletedAt is null
            where c.user.userId = :userId
            group by c.categoryId
            """)
    List<CategoryTodoCount> countTodosByCategory(@Param("userId") Long userId);

    @Query("select t from Todo t where t.user.userId = :userId and t.deletedAt is null order by t.dueDate asc")
    List<Todo> findByUserIdAndNotDeleted(@Param("userId") Long userId);

    /** 카테고리 삭제 시 해당 카테고리를 쓰던 todo 전체를 카테고리 없음 상태로 해제한다 */
    @Modifying
    @Query("update Todo t set t.category = null where t.category.categoryId = :categoryId")
    void clearCategory(@Param("categoryId") Long categoryId);
}
