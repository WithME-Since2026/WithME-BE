package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;

class RecurrenceExpanderTest {

    private final RecurrenceExpander expander = new RecurrenceExpander();

    @Test
    void expandsOldDailyRuleFromRequestedRange() {
        Recurrence rule = rule(RecurrenceFreq.DAILY, 3, null, RecurrenceEndType.DATE,
                LocalDate.of(2026, 1, 8), null);

        List<LocalDate> result = expander.expand(
                rule,
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(result).containsExactly(
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 8)
        );
    }

    @Test
    void expandsMultipleDaysEveryOtherWeekInOrder() {
        Recurrence rule = rule(RecurrenceFreq.WEEKLY, 2, "MON,WED,FRI",
                RecurrenceEndType.NEVER, null, null);

        List<LocalDate> result = expander.expand(
                rule,
                LocalDate.of(2026, 1, 7),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 25)
        );

        assertThat(result).containsExactly(
                LocalDate.of(2026, 1, 7),
                LocalDate.of(2026, 1, 9),
                LocalDate.of(2026, 1, 19),
                LocalDate.of(2026, 1, 21),
                LocalDate.of(2026, 1, 23)
        );
    }

    @Test
    void skipsMissingMonthlyDatesWithoutConsumingCount() {
        Recurrence rule = rule(RecurrenceFreq.MONTHLY, 1, null,
                RecurrenceEndType.COUNT, null, 3);

        List<LocalDate> result = expander.expand(
                rule,
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertThat(result).containsExactly(
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 5, 31)
        );
    }

    @Test
    void returnsEmptyWhenCountEndedBeforeRequestedRange() {
        Recurrence rule = rule(RecurrenceFreq.DAILY, 1, null,
                RecurrenceEndType.COUNT, null, 3);
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        assertThat(expander.expand(rule, anchor, anchor.plusDays(3), anchor.plusDays(10)))
                .isEmpty();
    }

    @Test
    void rejectsInvalidInterval() {
        Recurrence rule = rule(RecurrenceFreq.DAILY, 0, null,
                RecurrenceEndType.NEVER, null, null);

        assertInvalid(rule);
    }

    @Test
    void rejectsDuplicateWeeklyDays() {
        Recurrence rule = rule(RecurrenceFreq.WEEKLY, 1, "MON,MON",
                RecurrenceEndType.NEVER, null, null);

        assertInvalid(rule);
    }

    @Test
    void rejectsCountOverLimit() {
        Recurrence rule = rule(RecurrenceFreq.MONTHLY, 1, null,
                RecurrenceEndType.COUNT, null, 1001);

        assertInvalid(rule);
    }

    private void assertInvalid(Recurrence rule) {
        assertThatThrownBy(() -> expander.expand(
                rule,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 1)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_RECURRENCE);
    }

    private Recurrence rule(
            RecurrenceFreq freq,
            int interval,
            String byDays,
            RecurrenceEndType endType,
            LocalDate endDate,
            Integer endCount
    ) {
        return Recurrence.builder()
                .ownerType(RecurrenceOwnerType.SCHEDULE)
                .ownerId(1L)
                .freq(freq)
                .repeatInterval(interval)
                .byDays(byDays)
                .endType(endType)
                .endDate(endDate)
                .endCount(endCount)
                .build();
    }
}
