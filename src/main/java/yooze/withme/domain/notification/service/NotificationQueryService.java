package yooze.withme.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.notification.entity.Notification;
import yooze.withme.domain.notification.repository.NotificationRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    /** 내 알림 목록 조회 (최신순) */
    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId);
    }

    /** 안읽은 알림 수 */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserUserIdAndReadAtIsNull(userId);
    }
}
