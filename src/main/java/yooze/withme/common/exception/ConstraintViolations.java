package yooze.withme.common.exception;

import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;

/**
 * DB 유니크/무결성 제약 위반이 어떤 제약에서 발생했는지 판별
 * 애플리케이션 선검사는 동시 요청에서 모두 통과할 수 있으므로,
 * 최종 판정은 DB 제약 위반을 해석해서 내려야 함.
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    /**
     * DataIntegrityViolationException 뿐 아니라 Throwable 을 받는 이유:
     * DEFERRABLE 제약은 커밋 시점에 터지므로 TransactionSystemException 등
     * 다른 예외에 감싸여 올라온다.
     */
    public static boolean matches(Throwable e, String constraintName) {
        String target = constraintName.toLowerCase(Locale.ROOT);

        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }

            // 드라이버/방언에 따라 제약 이름이 예외 필드로 노출되지 않는 경우 메시지로 확인한다.
            // 제약 이름은 사람이 읽는 텍스트가 아니라 식별자이므로 기본 Locale 에 좌우되면 안 된다.
            // (터키어 Locale 에서는 'I' 가 'ı' 로 변환되어 'i' 와 일치하지 않는다.)
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(target)) {
                return true;
            }

            if (cause.getCause() == cause) {
                break;
            }
        }

        return false;
    }
}
