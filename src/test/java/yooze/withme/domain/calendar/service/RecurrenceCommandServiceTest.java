package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.RecurrenceRepository;

@ExtendWith(MockitoExtension.class)
class RecurrenceCommandServiceTest {

    private static final LocalDate ANCHOR = LocalDate.of(2026, 8, 19);

    @Mock
    private RecurrenceRepository recurrenceRepository;

    private RecurrenceCommandService recurrenceCommandService;

    @BeforeEach
    void setUp() {
        recurrenceCommandService = new RecurrenceCommandService(
                recurrenceRepository,
                new RecurrenceExpander()
        );
    }

    @Test
    void createsRuleForOwner() {
        when(recurrenceRepository.findByOwnerTypeAndOwnerId(
                RecurrenceOwnerType.SCHEDULE,
                10L
        )).thenReturn(Optional.empty());
        when(recurrenceRepository.save(any(Recurrence.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurrenceResponse response = recurrenceCommandService.upsert(
                RecurrenceOwnerType.SCHEDULE,
                10L,
                ANCHOR,
                weekly("MON,WED")
        );

        ArgumentCaptor<Recurrence> captor = ArgumentCaptor.forClass(Recurrence.class);
        verify(recurrenceRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(10L);
        assertThat(response.byDays()).isEqualTo("MON,WED");
    }

    @Test
    void rejectsWeeklyRuleMissingAnchorDay() {
        when(recurrenceRepository.findByOwnerTypeAndOwnerId(
                RecurrenceOwnerType.SCHEDULE,
                10L
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recurrenceCommandService.upsert(
                RecurrenceOwnerType.SCHEDULE,
                10L,
                ANCHOR,
                weekly("MON,FRI")
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_RECURRENCE);
        verify(recurrenceRepository, never()).save(any());
    }

    private RecurrenceRequest weekly(String byDays) {
        return new RecurrenceRequest(
                RecurrenceFreq.WEEKLY,
                1,
                byDays,
                RecurrenceEndType.NEVER,
                null,
                null
        );
    }
}
