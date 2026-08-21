package yooze.withme.domain.calendar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.calendar.entity.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    Optional<Holiday> findByDateAndName(LocalDate date, String name);

    boolean existsByDateBetween(LocalDate from, LocalDate to);
}
