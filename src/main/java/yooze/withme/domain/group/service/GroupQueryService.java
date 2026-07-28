package yooze.withme.domain.group.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.entity.Group;
import yooze.withme.domain.group.entity.GroupLocation;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.GroupMemberStatus;
import yooze.withme.domain.group.repository.GroupLocationRepository;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;
import yooze.withme.domain.group.repository.GroupRoundRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupQueryService {

    private final GroupRepository groupRepository;
    private final GroupLocationRepository groupLocationRepository;
    private final GroupRoundRepository groupRoundRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupResponseRepository groupResponseRepository;

    /** 모임 상세 조회 */
    public GroupDetailResponse getGroupDetail(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        GroupLocation location = groupLocationRepository.findByGroupId(groupId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        long memberCount = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);

        return GroupDetailResponse.of(group, location, memberCount);
    }

    /** 모임 내 회차 목록 조회 (날짜/시간 오름차순) */
    public List<GroupRoundResponse> getGroupRounds(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GeneralException(ErrorStatus.GROUP_NOT_FOUND);
        }
        return groupRoundRepository.findByGroupIdOrderByRoundDateAscRoundTimeAsc(groupId).stream()
                .map(GroupRoundResponse::from)
                .toList();
    }

    /** 특정 회차의 참석 현황 목록 조회 - OWNER/CO_OWNER만 접근 가능 */
    public List<AttendanceResponse> getGroupResponses(Long userId, Long roundId) {
        GroupRound round = groupRoundRepository.findById(roundId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_ROUND_NOT_FOUND));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(round.getGroup().getId(), userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
        if (!member.manager()) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_FORBIDDEN);
        }

        return groupResponseRepository.findByGroupRoundId(roundId).stream()
                .map(AttendanceResponse::from)
                .toList();
    }
}
