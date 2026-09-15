package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;
import yooze.withme.domain.calendar.dto.request.UpdateOccurrenceRequest;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.entity.RecurrenceException;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.RecurrenceExceptionRepository;
import yooze.withme.domain.calendar.repository.RecurrenceRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurrenceCommandService {

    private final RecurrenceRepository recurrenceRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final RecurrenceExpander recurrenceExpander;

    public RecurrenceResponse upsert(
            RecurrenceOwnerType ownerType,
            Long ownerId,
            LocalDate anchorDate,
            RecurrenceRequest request
    ) {
        if (request == null) {
            throw invalid();
        }

        Recurrence recurrence = recurrenceRepository
                .findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .orElseGet(() -> Recurrence.builder()
                        .ownerType(ownerType)
                        .ownerId(ownerId)
                        .build());

        recurrence.update(
                request.freq(),
                request.repeatInterval(),
                request.byDays(),
                request.endType(),
                request.endDate(),
                request.endCount()
        );
        validate(recurrence, anchorDate);
        Recurrence saved = recurrenceRepository.save(recurrence);
        dropStaleExceptions(saved, anchorDate);
        return RecurrenceResponse.from(saved);
    }

    /**
     * 새 규칙이 더 이상 만들어내지 않는 회차의 예외를 지운다.
     * 남겨두면 OVERRIDE 의 옮겨진 날짜만 보고 유령 회차가 되살아난다.
     */
    private void dropStaleExceptions(Recurrence recurrence, LocalDate anchorDate) {
        if (recurrence.getRecurrenceId() == null) {
            return;
        }
        // ponytail: 예외 1건당 하루짜리 전개 1번. 한 규칙의 예외 수만큼이라 실질적으로 몇 건이다.
        List<RecurrenceException> stale = recurrenceExceptionRepository
                .findByRecurrenceRecurrenceId(recurrence.getRecurrenceId()).stream()
                .filter(exception -> !producedBy(recurrence, anchorDate,
                        exception.getOccurrenceDate()))
                .toList();
        if (!stale.isEmpty()) {
            recurrenceExceptionRepository.deleteAll(stale);
        }
    }

    private boolean producedBy(
            Recurrence recurrence,
            LocalDate anchorDate,
            LocalDate occurrenceDate
    ) {
        return occurrenceDate != null
                && recurrenceExpander.expand(recurrence, anchorDate, occurrenceDate, occurrenceDate)
                .contains(occurrenceDate);
    }

    public RecurrenceResponse validateExisting(
            RecurrenceOwnerType ownerType,
            Long ownerId,
            LocalDate anchorDate
    ) {
        return recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(recurrence -> {
                    validate(recurrence, anchorDate);
                    return RecurrenceResponse.from(recurrence);
                })
                .orElse(null);
    }

    /**
     * 특정 회차 하나만 덮어쓴다. 전달한 필드만 덮어쓰고, 나머지는 기존 덮어쓰기 값을 유지한다.
     * {@code originStartTime}/{@code originEndTime} 은 덮어쓰기가 없을 때 화면에 찍히는 원본 시간으로,
     * 한쪽만 보내도 최종 시간이 뒤집히지 않는지 검증하는 데 쓴다(종일 항목이면 null).
     */
    public OccurrenceResponse override(
            RecurrenceOwnerType ownerType,
            Long ownerId,
            LocalDate anchorDate,
            LocalDate occurrenceDate,
            LocalTime originStartTime,
            LocalTime originEndTime,
            UpdateOccurrenceRequest request
    ) {
        if (request == null || request.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_OCCURRENCE);
        }

        RecurrenceException exception = exceptionOf(ownerType, ownerId, anchorDate, occurrenceDate);
        validateOverrideTimes(
                effective(request.startTime(), exception.getOverrideStartTime(), originStartTime),
                effective(request.endTime(), exception.getOverrideEndTime(), originEndTime)
        );
        exception.override(
                request.date(),
                request.title(),
                request.startTime(),
                request.endTime(),
                request.completed()
        );
        return OccurrenceResponse.from(recurrenceExceptionRepository.save(exception));
    }

    /** 특정 회차 하나만 건너뛴다. 원본과 나머지 회차는 그대로 둔다. */
    public void skip(
            RecurrenceOwnerType ownerType,
            Long ownerId,
            LocalDate anchorDate,
            LocalDate occurrenceDate
    ) {
        RecurrenceException exception = exceptionOf(ownerType, ownerId, anchorDate, occurrenceDate);
        exception.skip();
        recurrenceExceptionRepository.save(exception);
    }

    public void delete(RecurrenceOwnerType ownerType, Long ownerId) {
        recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .ifPresent(recurrenceRepository::delete);
    }

    /** 규칙이 실제로 만들어내는 회차인지 확인하고, 없으면 새 예외 행을 만들어 돌려준다. */
    private RecurrenceException exceptionOf(
            RecurrenceOwnerType ownerType,
            Long ownerId,
            LocalDate anchorDate,
            LocalDate occurrenceDate
    ) {
        Recurrence recurrence = recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.OCCURRENCE_NOT_FOUND));
        if (occurrenceDate == null
                || !recurrenceExpander.expand(recurrence, anchorDate, occurrenceDate, occurrenceDate)
                .contains(occurrenceDate)) {
            throw new GeneralException(ErrorStatus.OCCURRENCE_NOT_FOUND);
        }
        return recurrenceExceptionRepository
                .findByRecurrenceRecurrenceIdAndOccurrenceDate(
                        recurrence.getRecurrenceId(),
                        occurrenceDate
                )
                .orElseGet(() -> RecurrenceException.builder()
                        .recurrence(recurrence)
                        .occurrenceDate(occurrenceDate)
                        .build());
    }

    /** 덮어쓰기 이후 실제로 쓰이는 값: 이번 요청 > 기존 덮어쓰기 > 원본 순. */
    private LocalTime effective(LocalTime requested, LocalTime overridden, LocalTime origin) {
        if (requested != null) {
            return requested;
        }
        return overridden != null ? overridden : origin;
    }

    private void validateOverrideTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new GeneralException(ErrorStatus.INVALID_OCCURRENCE);
        }
    }

    private void validate(Recurrence recurrence, LocalDate anchorDate) {
        boolean anchorIncluded = recurrenceExpander
                .expand(recurrence, anchorDate, anchorDate, anchorDate)
                .contains(anchorDate);
        if (recurrence.getFreq() == RecurrenceFreq.WEEKLY && !anchorIncluded) {
            throw invalid();
        }
    }

    private GeneralException invalid() {
        return new GeneralException(ErrorStatus.INVALID_RECURRENCE);
    }
}
