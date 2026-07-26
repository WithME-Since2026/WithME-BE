package yooze.withme.domain.group.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupResponse;

public interface GroupResponseRepository extends JpaRepository<GroupResponse, Long> {

    @Query("select gr from GroupResponse gr join fetch gr.member where gr.groupRound.id = :roundId")
    List<GroupResponse> findByGroupRoundId(@Param("roundId") Long roundId);

    @Query("select gr from GroupResponse gr join fetch gr.member where gr.groupRound.id = :roundId and gr.member.id = :memberId")
    Optional<GroupResponse> findByGroupRoundIdAndMemberId(@Param("roundId") Long roundId, @Param("memberId") Long memberId);
}
