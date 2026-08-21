package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.RecurrenceExceptionRepository;
import yooze.withme.domain.calendar.repository.RecurrenceRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurrenceQueryService {

    private final RecurrenceRepository recurrenceRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final RecurrenceExpander recurrenceExpander;

    /** 반복 규칙을 찾는다. 반복이 아니면 null. */
    public RecurrenceResponse find(RecurrenceOwnerType ownerType, Long ownerId) {
        return recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(RecurrenceResponse::from)
                .orElse(null);
    }

    /** 조회 구간 안의 회차를 예외까지 적용해 전개한다. 반복이 아니면 빈 목록. */
    public List<OccurrenceResponse> findOccurrences(
            RecurrenceOwnerType ownerType,
            Long ownerId,
            LocalDate anchorDate,
            LocalDate from,
            LocalDate to
    ) {
        return recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(recurrence -> recurrenceExpander.applyExceptions(
                        recurrenceExpander.expand(recurrence, anchorDate, from, to),
                        recurrenceExceptionRepository
                                .findByRecurrenceRecurrenceId(recurrence.getRecurrenceId()),
                        from,
                        to
                ))
                .orElse(List.of());
    }
}
