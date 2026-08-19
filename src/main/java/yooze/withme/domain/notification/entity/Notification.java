package yooze.withme.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import yooze.withme.common.base.BaseEntity;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String body;

    /** null이면 읽지 않은 알림 */
    @Column
    private LocalDateTime readAt;

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }
}
