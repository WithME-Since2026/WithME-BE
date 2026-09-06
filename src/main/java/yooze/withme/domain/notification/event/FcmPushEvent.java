package yooze.withme.domain.notification.event;

/**
 * DB 커밋 후 FCM 푸시를 발송하기 위한 이벤트.
 * 트랜잭션 커밋이 완료된 뒤 리스너에서 처리되므로
 * DB 저장 실패 시 FCM이 발송되지 않는다.
 */
public record FcmPushEvent(
        Long userId,
        String title,
        String body
) {
}
