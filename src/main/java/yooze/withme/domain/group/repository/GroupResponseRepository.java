package yooze.withme.domain.group.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.group.enums.GroupMemberStatus;

public interface GroupResponseRepository extends JpaRepository<GroupResponse, Long> {

    @Query("select gr from GroupResponse gr join fetch gr.member where gr.groupRound.id = :roundId")
    List<GroupResponse> findByGroupRoundId(@Param("roundId") Long roundId);

    @Query("select gr from GroupResponse gr join fetch gr.member where gr.groupRound.id = :roundId and gr.member.id = :memberId")
    Optional<GroupResponse> findByGroupRoundIdAndMemberId(@Param("roundId") Long roundId, @Param("memberId") Long memberId);

    /** 캘린더에 붙일 내 참석 상태를 회차 목록 단위로 한 번에 읽는다. */
    @Query("""
        select gr from GroupResponse gr
        where gr.member.userId = :userId
          and gr.groupRound.id in :roundIds
        """)
    List<GroupResponse> findByUserIdAndGroupRoundIdIn(
            @Param("userId") Long userId,
            @Param("roundIds") Collection<Long> roundIds
    );

    /** 주차별 참석률용 — 기간 안 회차의 내 응답. 회차 날짜로 주를 나누므로 회차를 함께 읽는다 */
    @Query("""
        select gr from GroupResponse gr
        join fetch gr.groupRound r
        where gr.member.userId = :userId
          and gr.member.status = :memberStatus
          and r.roundDate between :from and :to
        """)
    List<GroupResponse> findByUserIdAndRoundDateBetween(
            @Param("userId") Long userId,
            @Param("memberStatus") GroupMemberStatus memberStatus,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Modifying
    @Query("delete from GroupResponse gr where gr.member.id in :memberIds")
    void deleteByMemberIdIn(@Param("memberIds") Collection<Long> memberIds);

    @Modifying
    @Query("delete from GroupResponse gr where gr.groupRound.id in (select r.id from GroupRound r where r.group.id = :groupId)")
    void deleteByGroupId(@Param("groupId") Long groupId);

    @Query("""
        select count(gr)
        from GroupResponse gr
        where gr.member.userId = :userId
          and gr.member.status = :memberStatus
          and gr.attendanceStatus = :attendanceStatus
        """)
    long countAttendance(
            @Param("userId") Long userId,
            @Param("memberStatus") GroupMemberStatus memberStatus,
            @Param("attendanceStatus") AttendanceStatus attendanceStatus
    );
}
