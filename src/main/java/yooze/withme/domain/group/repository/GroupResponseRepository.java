package yooze.withme.domain.group.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.group.entity.GroupResponse;

public interface GroupResponseRepository extends JpaRepository<GroupResponse, Long> {

    List<GroupResponse> findByGroupRoundId(Long roundId);

    Optional<GroupResponse> findByGroupRoundIdAndMemberId(Long roundId, Long memberId);
}
