package yooze.withme.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.base.BaseStatus;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.todo.entity.Category;

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("[*] DataIntegrityViolationException : {}", e.getMostSpecificCause().getMessage());

        if (ConstraintViolations.matches(e, "uq_user_auth_provider_login_id")) {
            return ApiResponse.error(ErrorStatus.DUPLICATE_ID);
        }
        if (ConstraintViolations.matches(e, Category.UK_CATEGORY_USER_NAME)) {
            return ApiResponse.error(ErrorStatus.DUPLICATE_CATEGORY_NAME);
        }
        if (ConstraintViolations.matches(e, Category.UK_CATEGORY_USER_SORT_ORDER)) {
            return ApiResponse.error(ErrorStatus.CATEGORY_ORDER_CONFLICT);
        }
        return ApiResponse.error(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * DEFERRABLE 제약 위반은 서비스 메서드가 끝난 뒤 커밋 시점에 터지므로
     * 서비스의 try/catch 가 아니라 여기까지 올라온다.
     * 커밋 실패 전반을 감싸는 예외이므로, 우리가 아는 제약일 때만 해석하고
     * 나머지는 기존대로 500 으로 둔다.
     */
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystemException(
            TransactionSystemException e) {
        if (ConstraintViolations.matches(e, Category.UK_CATEGORY_USER_SORT_ORDER)) {
            log.warn("[*] 카테고리 정렬 순서 충돌 : {}", e.getMessage());
            return ApiResponse.error(ErrorStatus.CATEGORY_ORDER_CONFLICT);
        }
        if (ConstraintViolations.matches(e, Category.UK_CATEGORY_USER_NAME)) {
            log.warn("[*] 카테고리 이름 중복 : {}", e.getMessage());
            return ApiResponse.error(ErrorStatus.DUPLICATE_CATEGORY_NAME);
        }
        log.error("[*] TransactionSystemException :", e);
        return ApiResponse.error(ErrorStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException e) {
        log.warn("[*] ObjectOptimisticLockingFailureException : {}", e.getMessage());
        return ApiResponse.error(ErrorStatus.GROUP_RESPONSE_CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException e) {
        String errorMessage = "잘못된 요청입니다: " + e.getMessage();
        log.error("[*] IllegalArgumentException :", e);
        return ApiResponse.error(ErrorStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(
            NullPointerException e) {
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
        String errorMessage;
        if (ex.getBindingResult().getFieldErrors().isEmpty()) {
            errorMessage = errorCode.getMessage();
        } else {
            errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }

        ApiResponse<Void> body = createApiResponse(errorCode, errorMessage);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("[*] HttpMessageNotReadableException : {}", ex.getMessage());
        BaseStatus errorCode = ErrorStatus.BAD_REQUEST;
        ApiResponse<Void> body = createApiResponse(errorCode, null);
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

    private ApiResponse<Void> createApiResponse(
            BaseStatus errorStatus, String errorMessage
    ) {
        return new ApiResponse<>(
                false,
                errorStatus.getCode(),
                (errorMessage != null ? errorMessage : errorStatus.getMessage()),
                null
        );
    }
}
