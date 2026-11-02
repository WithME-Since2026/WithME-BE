package yooze.withme.common.exception;

import lombok.Getter;
import yooze.withme.common.base.BaseStatus;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseStatus errorStatus;
    /** 에러 응답의 data 로 함께 내려줄 값. 없으면 null */
    private final Object data;

    public GeneralException(BaseStatus errorStatus) {
        this(errorStatus, null);
    }

    public GeneralException(BaseStatus errorStatus, Object data) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
        this.data = data;
    }
}
