package yooze.withme.domain.calendar.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.calendar.entity.Schedule;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 캘린더 구간에 걸리는 개인 일정.
     * 비반복은 기간이 겹치는 것만, 반복은 시작일이 구간 종료일 이전이면 모두 가져온다
     * (원본 시작일이 한참 전이어도 회차는 구간 안에 들어올 수 있다).
     */
    @Query("""
            select s from Schedule s
            where s.user.userId = :userId
              and s.deletedAt is null
              and s.startDate <= :to
              and (s.endDate >= :from
                   or exists (select 1 from Recurrence r
                              where r.ownerType = yooze.withme.domain.calendar.enums.RecurrenceOwnerType.SCHEDULE
                                and r.ownerId = s.scheduleId))
            """)
    List<Schedule> findForCalendar(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
