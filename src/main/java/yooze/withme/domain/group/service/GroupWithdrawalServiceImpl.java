package yooze.withme.domain.group.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.enums.GroupMemberPosition;
import yooze.withme.domain.group.enums.GroupMemberStatus;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class GroupWithdrawalServiceImpl implements GroupWithdrawalService {

    // 멤버 상태와 참석 응답은 하나의 모임 탈퇴 작업에서 함께 변경한다.
    private final GroupMemberRepository groupMemberRepository;
    private final GroupResponseRepository groupResponseRepository;

    /** 본인이 속한 모임에서 탈퇴하고 과거·현재 참석 응답을 삭제한다. */
    @Override
    public void withdrawUser(Long userId) {
        List<GroupMember> members = groupMemberRepository.findAllByUserId(userId);
        for (GroupMember member : members) {
            if (member.getStatus() == GroupMemberStatus.ACTIVE
                    && member.getPosition() == GroupMemberPosition.OWNER
                    && groupMemberRepository.countByGroupIdAndStatus(member.getGroup().getId(), GroupMemberStatus.ACTIVE) > 1) {
                throw new GeneralException(ErrorStatus.GROUP_OWNER_TRANSFER_REQUIRED);
            }
        }
        groupResponseRepository.deleteAllByUserId(userId);
        members.forEach(GroupMember::leave);
    }
}
