package yooze.withme.domain.group.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.enums.GroupMemberStatus;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupIdAndStatus(Long groupId, GroupMemberStatus status);
}
