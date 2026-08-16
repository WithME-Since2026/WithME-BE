package yooze.withme.domain.calendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.calendar.entity.Schedule;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
}
