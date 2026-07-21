package yooze.withme.common.status.success;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import yooze.withme.common.status.BaseStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseStatus {

    SUCCESS_200("WITHME_200", HttpStatus.OK, "성공입니다."),
    SUCCESS_201("WITHME_201", HttpStatus.CREATED, "성공입니다."),
    SUCCESS_204("WITHME_204", HttpStatus.NO_CONTENT, "성공입니다."),

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
