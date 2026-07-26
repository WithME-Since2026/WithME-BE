package yooze.withme.common.status.success;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import yooze.withme.common.status.BaseStatus;


@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {
    SUCCESS_200("WITHME_200", HttpStatus.OK, "성공적으로 처리했습니다."),
    SUCCESS_201("WITHME_201", HttpStatus.CREATED, "성공적으로 생성했습니다."),
    SUCCESS_204("WITHME_204", HttpStatus.NO_CONTENT, "성공적으로 삭제했습니다."),

    /**
     * Auth
     */
    LOGIN_SUCCESS("AUTH_200", HttpStatus.OK, "로그인 성공"),
    LOGOUT_SUCCESS("AUTH_200", HttpStatus.OK, "로그아웃 성공"),
    CREATE_USER_SUCCESS("AUTH_201", HttpStatus.CREATED, "회원가입 성공"),
    UPDATE_PASSWORD_SUCCESS("AUTH_204", HttpStatus.OK, "비밀번호 변경 성공"),
    CREATE_TOKEN_SUCCESS("AUTH_200", HttpStatus.OK, "토큰 재발급 성공"),

    /**
     * Group
     */
    CREATE_GROUP_SUCCESS("GROUP_201", HttpStatus.CREATED, "모임 생성 성공"),
    GET_GROUP_SUCCESS("GROUP_200", HttpStatus.OK, "모임 상세 조회 성공"),
    GET_GROUP_ROUNDS_SUCCESS("GROUP_200", HttpStatus.OK, "모임 회차 목록 조회 성공"),
    RESCHEDULE_GROUP_ROUND_SUCCESS("GROUP_200", HttpStatus.OK, "모임 일정 변경 성공"),
    GET_GROUP_RESPONSES_SUCCESS("GROUP_200", HttpStatus.OK, "참석 현황 조회 성공"),
    SUBMIT_GROUP_RESPONSE_SUCCESS("GROUP_200", HttpStatus.OK, "참석 응답 제출 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
