package yooze.withme.domain.group.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupRound;

public interface GroupRoundRepository extends JpaRepository<GroupRound, Long> {

    List<GroupRound> findByGroupIdOrderByRoundDateAscRoundTimeAsc(Long groupId);

    /** D-1 리마인더용 — 특정 날짜에 예정된 모든 회차 */
    @Query("SELECT gr FROM GroupRound gr JOIN FETCH gr.group WHERE gr.roundDate = :date")
    List<GroupRound> findByRoundDateWithGroup(@Param("date") LocalDate date);
}
