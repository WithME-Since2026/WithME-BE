package yooze.withme.domain.group.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.enums.GroupMemberStatus;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupIdAndStatus(Long groupId, GroupMemberStatus status);

    @Query("select gm from GroupMember gm join fetch gm.group where gm.userId = :userId and gm.status = :status")
    List<GroupMember> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") GroupMemberStatus status);

    /** 회원 탈퇴용 — 상태와 무관하게 유저의 모든 멤버십 */
    @Query("select gm from GroupMember gm join fetch gm.group where gm.userId = :userId")
    List<GroupMember> findAllByUserIdWithGroup(@Param("userId") Long userId);

    @Modifying
    @Query("delete from GroupMember gm where gm.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);

    /** 스케줄러용 — 특정 그룹의 활성 멤버 userId 목록 */
    @Query("SELECT gm.userId FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.status = 'ACTIVE'")
    List<Long> findActiveUserIdsByGroupId(@Param("groupId") Long groupId);
}
