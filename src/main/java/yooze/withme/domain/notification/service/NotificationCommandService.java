package yooze.withme.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.notification.entity.Notification;
import yooze.withme.domain.notification.enums.NotificationType;
import yooze.withme.domain.notification.event.FcmPushEvent;
import yooze.withme.domain.notification.repository.FcmTokenRepository;
import yooze.withme.domain.notification.repository.NotificationRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 알림 DB 저장 후 FCM 푸시 이벤트 발행.
     * FCM 발송은 트랜잭션 커밋 완료 후 실행되므로
     * DB 저장 실패 시 푸시가 나가지 않는다.
     */
    public void send(User user, NotificationType type, String title, String body) {
        // 1. DB 저장
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .build());

        // 2. 알림 설정 확인 후 이벤트 발행 (FCM은 커밋 후 처리)
        boolean agreed = switch (type) {
            case GROUP_REMINDER -> user.isNotifyGroupRemind();
            case TODO_DEADLINE -> user.isNotifyTodoDeadline();
            case GROUP_INVITE -> user.isNotifyGroupInvite();
        };
        if (!agreed) {
            log.debug("[*] 알림 수신 비동의 - userId: {}, type: {}", user.getUserId(), type);
            return;
        }

        eventPublisher.publishEvent(new FcmPushEvent(user.getUserId(), title, body));
    }

    /** FCM 토큰 등록 또는 갱신 — 기기 단위 원자적 upsert */
    public void registerFcmToken(User user, String deviceId, String token) {
        fcmTokenRepository.upsert(user.getUserId(), deviceId, token);
    }

    /** 단건 읽음 처리 (본인 알림인지 확인) */
    public void markAsRead(Long userId, Long notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getUserId().equals(userId))
                .ifPresent(Notification::markAsRead);
    }

    /** 전체 읽음 처리 */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
