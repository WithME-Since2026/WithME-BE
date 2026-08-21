package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.RecurrenceRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurrenceCommandService {

    private final RecurrenceRepository recurrenceRepository;
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
        return RecurrenceResponse.from(recurrenceRepository.save(recurrence));
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

    public void delete(RecurrenceOwnerType ownerType, Long ownerId) {
        recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .ifPresent(recurrenceRepository::delete);
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
