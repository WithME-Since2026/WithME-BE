package yooze.withme.domain.group.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupRound;

public interface GroupRoundRepository extends JpaRepository<GroupRound, Long> {

    List<GroupRound> findByGroupIdOrderByRoundDateAscRoundTimeAsc(Long groupId);

    /** 캘린더 구간 안의 내 모임 회차. 제목에 쓸 모임 이름까지 함께 읽는다. */
    @Query("""
            select r from GroupRound r
            join fetch r.group
            where r.group.id in :groupIds
              and r.roundDate between :from and :to
            """)
    List<GroupRound> findByGroupIdsAndRoundDateBetween(
            @Param("groupIds") Collection<Long> groupIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );


    /** D-1 리마인더용 — 특정 날짜에 예정된 모든 회차 */
    @Query("SELECT gr FROM GroupRound gr JOIN FETCH gr.group WHERE gr.roundDate = :date")
    List<GroupRound> findByRoundDateWithGroup(@Param("date") LocalDate date);
}
