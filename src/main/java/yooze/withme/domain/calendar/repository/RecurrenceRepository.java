package yooze.withme.domain.calendar.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;

public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {

    Optional<Recurrence> findByOwnerTypeAndOwnerId(
            RecurrenceOwnerType ownerType,
            Long ownerId
    );
}
