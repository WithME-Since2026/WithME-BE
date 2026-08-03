package yooze.withme.domain.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.common.base.BaseEntity;
import yooze.withme.domain.auth.entity.User;

@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Todo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    private Long todoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 카테고리는 선택 사항이며, 카테고리 삭제 시 null 로 해제된다 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "notification_status", nullable = false)
    @Builder.Default
    private boolean notificationStatus = false;

    /** 제목/마감일/알림여부 부분 수정 — null 인 필드는 기존 값을 유지한다 */
    public void update(String title, LocalDate dueDate, Boolean notificationStatus) {
        if (title != null) {
            this.title = title;
        }
        if (dueDate != null) {
            this.dueDate = dueDate;
        }
        if (notificationStatus != null) {
            this.notificationStatus = notificationStatus;
        }
    }

    /** 카테고리 변경 — null 을 넘기면 카테고리 없음으로 해제된다 */
    public void changeCategory(Category category) {
        this.category = category;
    }

    public void updateCompleted(boolean completed) {
        this.completed = completed;
    }

    /** 소프트 삭제 */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean deleted() {
        return this.deletedAt != null;
    }
}