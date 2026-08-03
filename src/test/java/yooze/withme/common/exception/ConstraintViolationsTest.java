package yooze.withme.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;

class ConstraintViolationsTest {

    private final Locale originalLocale = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void matchesIsIndependentOfDefaultLocale() {
        // 터키어 Locale 에서는 'I' 가 'ı' 로 소문자화되어 'i' 와 일치하지 않는다.
        Locale.setDefault(Locale.forLanguageTag("tr"));

        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException(
                        "ERROR: duplicate key value violates unique constraint \"idx_user_name\""
                )
        );

        assertThat(ConstraintViolations.matches(e, "IDX_USER_NAME")).isTrue();
    }

    @Test
    void matchesFindsConstraintWrappedInCommitTimeException() {
        // DEFERRABLE 제약은 커밋 시점에 터져 TransactionSystemException 에 감싸여 올라온다.
        TransactionSystemException e = new TransactionSystemException(
                "Could not commit JPA transaction",
                new SQLException(
                        "ERROR: duplicate key value violates unique constraint "
                                + "\"uk_category_user_sort_order\""
                )
        );

        assertThat(ConstraintViolations.matches(e, "uk_category_user_sort_order")).isTrue();
        assertThat(ConstraintViolations.matches(e, "uk_category_user_name")).isFalse();
    }

    @Test
    void matchesReturnsFalseForUnrelatedConstraint() {
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException(
                        "ERROR: duplicate key value violates unique constraint \"fk_category_user\""
                )
        );

        assertThat(ConstraintViolations.matches(e, "uk_category_user_name")).isFalse();
    }
}
