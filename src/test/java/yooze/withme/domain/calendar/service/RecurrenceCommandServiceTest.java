package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import yooze.withme.domain.calendar.dto.request.UpdateOccurrenceRequest;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.entity.RecurrenceException;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.enums.RecurrenceExceptionType;
import yooze.withme.domain.calendar.repository.RecurrenceExceptionRepository;
import yooze.withme.domain.calendar.repository.RecurrenceRepository;

@ExtendWith(MockitoExtension.class)
class RecurrenceCommandServiceTest {

    private static final LocalDate ANCHOR = LocalDate.of(2026, 8, 19);

    @Mock
    private RecurrenceRepository recurrenceRepository;

    @Mock
    private RecurrenceExceptionRepository recurrenceExceptionRepository;

    private RecurrenceCommandService recurrenceCommandService;

    @BeforeEach
    void setUp() {
        recurrenceCommandService = new RecurrenceCommandService(
                recurrenceRepository,
                recurrenceExceptionRepository,
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

    @Test
    void overridesSingleOccurrence() {
        givenWeeklyRule();
        when(recurrenceExceptionRepository.findByRecurrenceRecurrenceIdAndOccurrenceDate(
                7L,
                ANCHOR.plusWeeks(1)
        )).thenReturn(Optional.empty());
        when(recurrenceExceptionRepository.save(any(RecurrenceException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OccurrenceResponse response = recurrenceCommandService.override(
                RecurrenceOwnerType.SCHEDULE,
                10L,
                ANCHOR,
                ANCHOR.plusWeeks(1),
                null,
                null,
                new UpdateOccurrenceRequest("변경된 제목", ANCHOR.plusWeeks(1).plusDays(1),
                        null, null, null)
        );

        assertThat(response.occurrenceDate()).isEqualTo(ANCHOR.plusWeeks(1));
        assertThat(response.date()).isEqualTo(ANCHOR.plusWeeks(1).plusDays(1));
        assertThat(response.title()).isEqualTo("변경된 제목");
    }

    @Test
    void keepsPreviousOverrideValuesWhenRequestOmitsThem() {
        when(recurrenceRepository.findByOwnerTypeAndOwnerId(RecurrenceOwnerType.TODO, 10L))
                .thenReturn(Optional.of(Recurrence.builder()
                        .recurrenceId(7L)
                        .ownerType(RecurrenceOwnerType.TODO)
                        .ownerId(10L)
                        .freq(RecurrenceFreq.WEEKLY)
                        .repeatInterval(1)
                        .byDays("WED")
                        .endType(RecurrenceEndType.NEVER)
                        .build()));
        RecurrenceException existing = RecurrenceException.builder()
                .occurrenceDate(ANCHOR)
                .exceptionType(RecurrenceExceptionType.OVERRIDE)
                .overrideTitle("이 회차만 바꾼 제목")
                .build();
        when(recurrenceExceptionRepository.findByRecurrenceRecurrenceIdAndOccurrenceDate(
                7L,
                ANCHOR
        )).thenReturn(Optional.of(existing));
        when(recurrenceExceptionRepository.save(any(RecurrenceException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 체크박스만 토글: completed 만 담긴 요청이 와도 제목 덮어쓰기는 남아야 한다
        OccurrenceResponse response = recurrenceCommandService.override(
                RecurrenceOwnerType.TODO,
                10L,
                ANCHOR,
                ANCHOR,
                null,
                null,
                new UpdateOccurrenceRequest(null, null, null, null, true)
        );

        assertThat(response.title()).isEqualTo("이 회차만 바꾼 제목");
        assertThat(response.completed()).isTrue();
    }

    @Test
    void rejectsOneSidedTimeThatInvertsFinalRange() {
        givenWeeklyRule();
        when(recurrenceExceptionRepository.findByRecurrenceRecurrenceIdAndOccurrenceDate(7L, ANCHOR))
                .thenReturn(Optional.empty());

        // 원본 14:00~15:00 인 회차에 endTime 만 13:00 으로 보내면 14:00~13:00 이 된다
        assertThatThrownBy(() -> recurrenceCommandService.override(
                RecurrenceOwnerType.SCHEDULE,
                10L,
                ANCHOR,
                ANCHOR,
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                new UpdateOccurrenceRequest(null, null, null, LocalTime.of(13, 0), null)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_OCCURRENCE);
        verify(recurrenceExceptionRepository, never()).save(any());
    }

    @Test
    void skipsSingleOccurrence() {
        givenWeeklyRule();
        when(recurrenceExceptionRepository.findByRecurrenceRecurrenceIdAndOccurrenceDate(
                7L,
                ANCHOR
        )).thenReturn(Optional.empty());

        recurrenceCommandService.skip(RecurrenceOwnerType.SCHEDULE, 10L, ANCHOR, ANCHOR);

        ArgumentCaptor<RecurrenceException> captor =
                ArgumentCaptor.forClass(RecurrenceException.class);
        verify(recurrenceExceptionRepository).save(captor.capture());
        assertThat(captor.getValue().getExceptionType())
                .isEqualTo(RecurrenceExceptionType.SKIP);
    }

    @Test
    void rejectsDateThatRuleNeverProduces() {
        givenWeeklyRule();

        assertThatThrownBy(() -> recurrenceCommandService.skip(
                RecurrenceOwnerType.SCHEDULE,
                10L,
                ANCHOR,
                ANCHOR.plusDays(1)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.OCCURRENCE_NOT_FOUND);
        verify(recurrenceExceptionRepository, never()).save(any());
    }

    @Test
    void rejectsEmptyOverride() {
        assertThatThrownBy(() -> recurrenceCommandService.override(
                RecurrenceOwnerType.SCHEDULE,
                10L,
                ANCHOR,
                ANCHOR,
                null,
                null,
                new UpdateOccurrenceRequest(null, null, null, null, null)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_OCCURRENCE);
        verify(recurrenceExceptionRepository, never()).save(any());
    }

    @Test
    void dropsExceptionsThatNewRuleNoLongerProduces() {
        givenWeeklyRule();
        when(recurrenceRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        RecurrenceException moved = RecurrenceException.builder()
                .occurrenceDate(LocalDate.of(2026, 8, 26))   // 격주로 바꾸면 사라지는 회차
                .exceptionType(RecurrenceExceptionType.OVERRIDE)
                .overrideDate(LocalDate.of(2026, 9, 1))
                .build();
        RecurrenceException kept = RecurrenceException.builder()
                .occurrenceDate(LocalDate.of(2026, 9, 2))    // 격주로 바꿔도 남는 회차
                .exceptionType(RecurrenceExceptionType.SKIP)
                .build();
        when(recurrenceExceptionRepository.findByRecurrenceRecurrenceId(7L))
                .thenReturn(List.of(moved, kept));

        recurrenceCommandService.upsert(
                RecurrenceOwnerType.SCHEDULE, 10L, ANCHOR,
                new RecurrenceRequest(RecurrenceFreq.WEEKLY, 2, "WED",
                        RecurrenceEndType.NEVER, null, null));

        ArgumentCaptor<List<RecurrenceException>> captor = ArgumentCaptor.forClass(List.class);
        verify(recurrenceExceptionRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(moved);
    }

    private void givenWeeklyRule() {
        when(recurrenceRepository.findByOwnerTypeAndOwnerId(
                RecurrenceOwnerType.SCHEDULE,
                10L
        )).thenReturn(Optional.of(Recurrence.builder()
                .recurrenceId(7L)
                .ownerType(RecurrenceOwnerType.SCHEDULE)
                .ownerId(10L)
                .freq(RecurrenceFreq.WEEKLY)
                .repeatInterval(1)
                .byDays("WED")
                .endType(RecurrenceEndType.NEVER)
                .build()));
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
