package yooze.withme.domain.group.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.enums.GroupMemberStatus;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupIdAndStatus(Long groupId, GroupMemberStatus status);

    @Query("select gm from GroupMember gm join fetch gm.group where gm.userId = :userId and gm.status = :status")
    List<GroupMember> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") GroupMemberStatus status);

    /** 스케줄러용 — 특정 그룹의 활성 멤버 userId 목록 */
    @Query("SELECT gm.userId FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.status = 'ACTIVE'")
    List<Long> findActiveUserIdsByGroupId(@Param("groupId") Long groupId);
}
