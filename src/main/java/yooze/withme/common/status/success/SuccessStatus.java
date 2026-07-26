package yooze.withme.common.status.success;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import yooze.withme.common.BaseStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {

    SUCCESS_200("WITHME_200", HttpStatus.OK, "성공입니다."),
    SUCCESS_201("WITHME_201", HttpStatus.CREATED, "성공입니다."),
    SUCCESS_204("WITHME_204", HttpStatus.NO_CONTENT, "성공입니다."),

    /**
     * Auth
     */
    LOGIN_SUCCESS("AUTH_200", HttpStatus.OK, "로그인 성공"),
    LOGOUT_SUCCESS("AUTH_200", HttpStatus.OK, "로그아웃 성공"),
    CREATE_USER_SUCCESS("AUTH_201", HttpStatus.CREATED, "회원가입 성공"),
    UPDATE_PASSWORD_SUCCESS("AUTH_200", HttpStatus.OK, "비밀번호 변경 성공"),
    CREATE_TOKEN_SUCCESS("AUTH_200", HttpStatus.OK, "토큰 재발급 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
