package yooze.withme.common;

import org.springframework.http.HttpStatus;

public interface BaseStatus {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
