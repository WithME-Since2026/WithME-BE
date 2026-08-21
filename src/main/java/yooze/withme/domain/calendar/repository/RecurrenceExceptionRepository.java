package yooze.withme.domain.calendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.calendar.entity.RecurrenceException;

public interface RecurrenceExceptionRepository extends JpaRepository<RecurrenceException, Long> {
}
