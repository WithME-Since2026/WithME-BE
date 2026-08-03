package yooze.withme.domain.todo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
