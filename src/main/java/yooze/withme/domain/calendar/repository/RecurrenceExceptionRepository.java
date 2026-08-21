package yooze.withme.domain.calendar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.calendar.entity.RecurrenceException;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {

    Optional<RecurrenceException> findByRecurrenceRecurrenceIdAndOccurrenceDate(
            Long recurrenceId,
            LocalDate occurrenceDate
    );

    // ponytail: 규칙 하나의 예외를 통째로 읽는다. override_date가 조회 구간 밖에서 안으로
    // 들어오는 경우까지 담으려면 구간 필터가 통하지 않기 때문. 예외가 많아지면
    // occurrence_date OR override_date 범위 조건으로 좁힐 것.
    List<RecurrenceException> findByRecurrenceRecurrenceId(Long recurrenceId);
}
