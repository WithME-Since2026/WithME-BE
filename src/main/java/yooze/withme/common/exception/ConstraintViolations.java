package yooze.withme.common.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DB 유니크/무결성 제약 위반이 어떤 제약에서 발생했는지 판별
 * 애플리케이션 선검사는 동시 요청에서 모두 통과할 수 있으므로,
 * 최종 판정은 DB 제약 위반을 해석해서 내려야 함.
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    public static boolean matches(DataIntegrityViolationException e, String constraintName) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }

        // 드라이버/방언에 따라 제약 이름이 예외 필드로 노출되지 않는 경우 메시지로 확인한다.
        String rootMessage = e.getMostSpecificCause().getMessage();
        return rootMessage != null
                && rootMessage.toLowerCase().contains(constraintName.toLowerCase());
    }
}
