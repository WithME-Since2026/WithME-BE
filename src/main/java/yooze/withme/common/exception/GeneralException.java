package yooze.withme.common.exception;

import lombok.Getter;
import yooze.withme.common.status.BaseStatus;

@Getter
public class GeneralException extends RuntimeException {

    private final BaseStatus errorStatus;

    public GeneralException(BaseStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
