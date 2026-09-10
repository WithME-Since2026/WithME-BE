package yooze.withme.domain.group.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
