package yooze.withme.common.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import yooze.withme.common.base.BaseStatus;


@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {

    SUCCESS_200("COMM_200", HttpStatus.OK, "요청을 성공했습니다."),
    SUCCESS_201("COMM_201", HttpStatus.CREATED, "요청을 생성했습니다."),
    SUCCESS_204("COMM_204", HttpStatus.NO_CONTENT, "요청을 성공했습니다."),

    /**
     * Auth
     */
    LOGIN_SUCCESS("AUTH_200", HttpStatus.OK, "로그인 성공"),
    LOGOUT_SUCCESS("AUTH_200", HttpStatus.OK, "로그아웃 성공"),
    CREATE_USER_SUCCESS("AUTH_201", HttpStatus.CREATED, "회원가입 성공"),
    UPDATE_PASSWORD_SUCCESS("AUTH_204", HttpStatus.OK, "비밀번호 변경 성공"),
    CREATE_TOKEN_SUCCESS("AUTH_200", HttpStatus.OK, "토큰 재발급 성공"),

    /**
     * CATEGORY
     */
    CREATE_CATEGORY_SUCCESS("CATEGORY_201", HttpStatus.CREATED, "카테고리 생성 성공"),
    UPDATE_CATEGORY_SUCCESS("CATEGORY_200", HttpStatus.OK, "카테고리 수정 성공"),
    GET_CATEGORIES_SUCCESS("CATEGORY_200", HttpStatus.OK, "카테고리 목록 조회 성공"),
    
    /**
     * User
     */
    GET_PROFILE_SUCCESS("USER_200", HttpStatus.OK, "프로필 조회 성공"),
    GET_NOTIFICATION_SUCCESS("USER_200", HttpStatus.OK, "알림 설정 조회 성공"),
    UPDATE_NOTIFICATION_SUCCESS("USER_200", HttpStatus.OK, "알림 설정 수정 성공"),
    GET_ATTENDANCE_SUCCESS("USER_200", HttpStatus.OK, "참여율 조회 성공"),
    GET_MY_GROUPS_SUCCESS("USER_200", HttpStatus.OK, "참여 모임 목록 조회 성공"),

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
