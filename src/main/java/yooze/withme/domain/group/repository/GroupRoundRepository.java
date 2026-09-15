package yooze.withme.domain.group.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
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
}
