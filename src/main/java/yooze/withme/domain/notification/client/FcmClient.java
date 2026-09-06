package yooze.withme.domain.notification.client;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
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
     * @return 토큰이 만료/무효화된 경우 false, 그 외(성공 또는 일시적 오류)는 true
     */
    public boolean send(String fcmToken, String title, String body) {
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
            return true;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("[*] FCM 토큰 만료/무효 - token: {}..., errorCode: {}", fcmToken.substring(0, Math.min(10, fcmToken.length())), errorCode);
                return false;
            }
            log.warn("[*] FCM 발송 실패 - errorCode: {}, error: {}", errorCode, e.getMessage());
            return true;
        }
    }
}
