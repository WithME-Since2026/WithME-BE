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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import yooze.withme.common.base.BaseEntity;
import yooze.withme.domain.auth.entity.User;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(
                name = Category.UK_CATEGORY_USER_NAME,
                columnNames = {"user_id", "category_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    public static final String DEFAULT_COLOR = "#FFAA03";

    /**
     * 사용자별 카테고리 이름 유니크 제약.
     * 이름 중복의 최종 판정 기준
     * 제약 위반을 DUPLICATE_CATEGORY_NAME 으로 변환할 때 이 상수를 사용.
     */
    public static final String UK_CATEGORY_USER_NAME = "uk_category_user_name";

    /**
     * 사용자별 정렬 순서 유니크 제약.
     * 재정렬 중간 상태에서 일시적으로 중복이 생기므로 DEFERRABLE 이어야 하고,
     * {@code @Table(uniqueConstraints)} 로는 표현할 수 없어 Flyway(V2)에서 생성한다.
     * 위반은 커밋 시점에 드러나므로 서비스가 아니라 예외 어드바이스에서 해석한다.
     */
    public static final String UK_CATEGORY_USER_SORT_ORDER = "uk_category_user_sort_order";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category_name", nullable = false, length = 64)
    private String categoryName;

    //todo : 나중에 색상값 정해지면 ENUM으로 설정
    @Column(name = "category_color", nullable = false, length = 7)
    @Builder.Default
    private String categoryColor = DEFAULT_COLOR;

    @Column(name = "sort_order", nullable = false)
    private Long sortOrder;

    public void update(String categoryName, String categoryColor, Long sortOrder) {
        if (categoryName != null) {
            this.categoryName = categoryName;
        }
        if (categoryColor != null) {
            this.categoryColor = categoryColor;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }
}
