package yooze.withme.domain.notification.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** FCM 푸시 알림 발송 클라이언트 */
@Slf4j
@Component
public class FcmClient {

    /**
     * 단일 기기에 푸시 알림 발송.
     *
     * @param fcmToken 수신 기기의 FCM 토큰
     * @param title    알림 제목
     * @param body     알림 본문
     */
    public void send(String fcmToken, String title, String body) {
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("[*] FCM 발송 성공 - messageId: {}", messageId);
        } catch (FirebaseMessagingException e) {
            // 푸시 실패가 로그인/알림 저장 자체를 막으면 안 되므로 예외를 삼킨다.
            log.warn("[*] FCM 발송 실패 - token: {}, error: {}", fcmToken, e.getMessage());
        }
    }
}
