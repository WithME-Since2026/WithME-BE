package yooze.withme.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.notification.client.FcmClient;
import yooze.withme.domain.notification.entity.FcmToken;
import yooze.withme.domain.notification.entity.Notification;
import yooze.withme.domain.notification.enums.NotificationType;
import yooze.withme.domain.notification.repository.FcmTokenRepository;
import yooze.withme.domain.notification.repository.NotificationRepository;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmClient fcmClient;

    /**
     * 알림 저장 + FCM 푸시 발송.
     * notifyAgree가 false인 유저는 DB 저장은 하되 푸시는 건너뛴다.
     */
    public void send(User user, NotificationType type, String title, String body) {
        // 1. DB 저장
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .build());

        // 2. FCM 푸시 (알림 설정 + 토큰 있을 때만)
        boolean agreed = switch (type) {
            case GROUP_REMINDER -> user.isNotifyGroupRemind();
            case TODO_DEADLINE -> user.isNotifyTodoDeadline();
            case GROUP_INVITE -> user.isNotifyGroupInvite();
        };
        if (!agreed) {
            log.debug("[*] 알림 수신 비동의 - userId: {}, type: {}", user.getUserId(), type);
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findAllByUser(user);
        if (tokens.isEmpty()) {
            log.debug("[*] FCM 토큰 없음 - userId: {}", user.getUserId());
            return;
        }
        tokens.forEach(t -> fcmClient.send(t.getToken(), title, body));
    }

    /** FCM 토큰 등록 또는 갱신 — 기기 단위 upsert */
    public void registerFcmToken(User user, String deviceId, String token) {
        fcmTokenRepository.findByUserAndDeviceId(user, deviceId)
                .ifPresentOrElse(
                        fcmToken -> fcmToken.updateToken(token),
                        () -> fcmTokenRepository.save(FcmToken.builder()
                                .user(user)
                                .deviceId(deviceId)
                                .token(token)
                                .build())
                );
    }

    /** 단건 읽음 처리 */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId)
                .ifPresent(Notification::markAsRead);
    }

    /** 전체 읽음 처리 */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
