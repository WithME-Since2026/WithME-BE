package yooze.withme.domain.calendar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.RecurrenceRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecurrenceQueryService {

    private final RecurrenceRepository recurrenceRepository;

    public RecurrenceResponse find(RecurrenceOwnerType ownerType, Long ownerId) {
        return recurrenceRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(RecurrenceResponse::from)
                .orElse(null);
    }
}
