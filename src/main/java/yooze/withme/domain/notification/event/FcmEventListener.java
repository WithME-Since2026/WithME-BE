package yooze.withme.domain.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.notification.client.FcmClient;
import yooze.withme.domain.notification.entity.FcmToken;
import yooze.withme.domain.notification.repository.FcmTokenRepository;

import java.util.List;

/**
 * FCM 푸시 이벤트 리스너.
 * DB 트랜잭션 커밋 완료 후 FCM을 발송하여
 * "푸시는 받았는데 알림 목록에 없는" 현상을 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmEventListener {

    private final FcmClient fcmClient;
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleFcmPush(FcmPushEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;

        List<FcmToken> tokens = fcmTokenRepository.findAllByUser(user);
        if (tokens.isEmpty()) {
            log.debug("[*] FCM 토큰 없음 - userId: {}", event.userId());
            return;
        }

        tokens.forEach(t -> {
            boolean valid = fcmClient.send(t.getToken(), event.title(), event.body());
            if (!valid) {
                log.info("[*] 만료된 FCM 토큰 삭제 - deviceId: {}", t.getDeviceId());
                fcmTokenRepository.delete(t);
            }
        });
    }
}
