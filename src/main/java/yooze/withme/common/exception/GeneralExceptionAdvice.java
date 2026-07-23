package yooze.withme.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.BaseStatus;
import yooze.withme.common.status.error.ErrorStatus;

@RestControllerAdvice
@Slf4j
public class GeneralExceptionAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException e) {
        if (e.getErrorStatus().getHttpStatus().is5xxServerError()) {
            log.error("[*] GeneralException :", e);
        } else {
            log.warn("[*] GeneralException : {}", e.getMessage());
        }
        return ApiResponse.error(e.getErrorStatus());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(ObjectOptimisticLockingFailureException e) {
        log.warn("[*] ObjectOptimisticLockingFailureException : {}", e.getMessage());
        return ApiResponse.error(ErrorStatus.GROUP_RESPONSE_CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        String errorMessage = "잘못된 요청입니다: " + e.getMessage();
        log.error("[*] IllegalArgumentException :", e);
        return ApiResponse.error(ErrorStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException e) {
        String errorMessage = "서버에서 예기치 않은 오류가 발생했습니다. 요청을 처리하는 중에 Null 값이 참조되었습니다.";
        log.error("[*] NullPointerException :", e);
        return ApiResponse.error(ErrorStatus.INTERNAL_SERVER_ERROR, errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[*] Internal Server Error :", e);
        return ApiResponse.error(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        BaseStatus errorCode = ErrorStatus.BAD_REQUEST;
        String errorMessage = ex.getBindingResult().getFieldErrors().isEmpty()
                ? errorCode.getMessage()
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        ApiResponse<Void> body = createApiResponse(errorCode, errorMessage);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        BaseStatus errorCode = ErrorStatus.METHOD_NOT_ALLOWED;
        ApiResponse<Void> body = createApiResponse(errorCode, null);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private ApiResponse<Void> createApiResponse(BaseStatus errorStatus, String errorMessage) {
        return new ApiResponse<>(
                false,
                errorStatus.getCode(),
                (errorMessage != null ? errorMessage : errorStatus.getMessage()),
                null
        );
    }
}
