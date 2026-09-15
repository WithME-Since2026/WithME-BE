package yooze.withme.domain.calendar.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;

public interface RecurrenceRepository extends JpaRepository<Recurrence, Long> {

    Optional<Recurrence> findByOwnerTypeAndOwnerId(
            RecurrenceOwnerType ownerType,
            Long ownerId
    );

    /** 목록 응답에 반복 여부를 붙일 때 원본 건수만큼 쿼리가 나가지 않게 한 번에 읽기 */
    List<Recurrence> findByOwnerTypeAndOwnerIdIn(
            RecurrenceOwnerType ownerType,
            Collection<Long> ownerIds
    );
}
