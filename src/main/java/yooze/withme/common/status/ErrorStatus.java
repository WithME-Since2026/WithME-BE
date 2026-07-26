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
