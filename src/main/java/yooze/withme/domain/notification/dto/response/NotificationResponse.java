package yooze.withme.domain.notification.dto.response;

import yooze.withme.domain.notification.entity.Notification;
import yooze.withme.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String body,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}
