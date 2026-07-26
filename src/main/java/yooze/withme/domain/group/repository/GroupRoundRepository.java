package yooze.withme.domain.group.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.group.entity.GroupRound;

public interface GroupRoundRepository extends JpaRepository<GroupRound, Long> {

    List<GroupRound> findByGroupIdOrderByRoundDateAscRoundTimeAsc(Long groupId);
}
