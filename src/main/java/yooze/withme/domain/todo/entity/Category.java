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
                name = "uk_category_user_name",
                columnNames = {"user_id", "category_name"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    public static final String DEFAULT_COLOR = "#FFAA03";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category_name", nullable = false, length = 64)
    private String categoryName;

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
