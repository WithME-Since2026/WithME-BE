package yooze.withme.common.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import yooze.withme.common.base.BaseStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseStatus {

    /**
     * Common
     */
    BAD_REQUEST("COMM_400", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED("COMM_401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("COMM_403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("COMM_404", HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("COMM_405", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다."),
    INTERNAL_SERVER_ERROR("COMM_500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    /**
     * Auth
     */
    INVALID_CREDENTIALS("AUTH_401", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN("AUTH_401", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN("AUTH_401", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    /**
     * User
     */
    USER_NOT_FOUND("USER_404", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_NOT_FOUND_BY_INFO("USER_404", HttpStatus.NOT_FOUND, "이름 또는 이메일이 일치하는 사용자가 없습니다."),
    DUPLICATE_EMAIL("USER_409", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_ID("USER_409", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    PASSWORD_MISMATCH("USER_400", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    INVALID_VERIFICATION_CODE("AUTH_400", HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않거나 만료되었습니다."),
    EMAIL_SEND_FAILED("COMM_500", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),
    TOO_MANY_REQUESTS("COMM_429", HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),

    /**
     * CATEGORY
     */
    CATEGORY_NOT_FOUND("CATEGORY_404", HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    CATEGORY_FORBIDDEN("CATEGORY_403", HttpStatus.FORBIDDEN, "본인의 카테고리만 수정할 수 있습니다."),
    DUPLICATE_CATEGORY_NAME("CATEGORY_409", HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다."),
    CATEGORY_ORDER_CONFLICT("CATEGORY_409", HttpStatus.CONFLICT,
            "다른 요청과 동시에 처리되어 카테고리 순서가 충돌했습니다. 다시 시도해주세요."),

    /**
     * Schedule
     */
    SCHEDULE_NOT_FOUND("SCHEDULE_404", HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."),
    SCHEDULE_FORBIDDEN("SCHEDULE_403", HttpStatus.FORBIDDEN, "본인의 일정만 접근할 수 있습니다."),
    INVALID_SCHEDULE_PERIOD("SCHEDULE_400", HttpStatus.BAD_REQUEST, "일정 기간이 올바르지 않습니다."),
    INVALID_RECURRENCE("RECURRENCE_400", HttpStatus.BAD_REQUEST, "반복 규칙이 올바르지 않습니다."),
    OCCURRENCE_NOT_FOUND("OCCURRENCE_404", HttpStatus.NOT_FOUND, "존재하지 않는 반복 회차입니다."),
    INVALID_OCCURRENCE("OCCURRENCE_400", HttpStatus.BAD_REQUEST, "회차 수정 요청이 올바르지 않습니다."),

    /**
     * Group
     */
    GROUP_NOT_FOUND("GROUP_404", HttpStatus.NOT_FOUND, "존재하지 않는 모임입니다."),
    GROUP_ROUND_NOT_FOUND("GROUP_404", HttpStatus.NOT_FOUND, "존재하지 않는 모임 회차입니다."),
    GROUP_MEMBER_NOT_FOUND("GROUP_404", HttpStatus.NOT_FOUND, "해당 모임의 멤버가 아닙니다."),
    GROUP_MEMBER_FORBIDDEN("GROUP_403", HttpStatus.FORBIDDEN, "모임 운영자만 접근할 수 있습니다."),
    GROUP_MEMBER_INACTIVE("GROUP_403", HttpStatus.FORBIDDEN, "비활성화되었거나 탈퇴한 멤버는 이용할 수 없습니다."),
    GROUP_RESPONSE_NOT_FOUND("GROUP_404", HttpStatus.NOT_FOUND, "출석 응답 정보를 찾을 수 없습니다."),
    INVALID_ATTENDANCE_STATUS("GROUP_400", HttpStatus.BAD_REQUEST, "참석 응답은 ATTEND 또는 ABSENT만 가능합니다."),
    GROUP_RESPONSE_CONFLICT("GROUP_409", HttpStatus.CONFLICT, "다른 요청과 동시에 처리되어 충돌이 발생했습니다. 다시 시도해주세요.");



    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
