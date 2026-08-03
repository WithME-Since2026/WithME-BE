package yooze.withme.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.todo.entity.Category;

class GeneralExceptionAdviceTest {

    private final GeneralExceptionAdvice advice = new GeneralExceptionAdvice();

    @Test
    void commitTimeSortOrderViolationBecomesConflict() {
        ResponseEntity<ApiResponse<Void>> response = advice.handleTransactionSystemException(
                commitFailure(Category.UK_CATEGORY_USER_SORT_ORDER)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode())
                .isEqualTo(ErrorStatus.CATEGORY_ORDER_CONFLICT.getCode());
    }

    @Test
    void unrelatedCommitFailureStaysServerError() {
        ResponseEntity<ApiResponse<Void>> response = advice.handleTransactionSystemException(
                new TransactionSystemException(
                        "Could not commit JPA transaction",
                        new SQLException("connection reset")
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private TransactionSystemException commitFailure(String constraintName) {
        return new TransactionSystemException(
                "Could not commit JPA transaction",
                new SQLException(
                        "ERROR: duplicate key value violates unique constraint \""
                                + constraintName + "\""
                )
        );
    }
}
