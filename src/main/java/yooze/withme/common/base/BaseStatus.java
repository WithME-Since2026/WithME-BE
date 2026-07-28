package yooze.withme.common.base;

import org.springframework.http.HttpStatus;

public interface BaseStatus {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
