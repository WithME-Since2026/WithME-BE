package yooze.withme.domain.group.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.dto.response.OwnerGroupResponse;
import yooze.withme.domain.group.entity.Group;
import yooze.withme.domain.group.entity.GroupLocation;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.group.enums.GroupMemberPosition;
import yooze.withme.domain.group.enums.GroupMemberStatus;
import yooze.withme.domain.group.repository.GroupLocationRepository;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;
import yooze.withme.domain.group.repository.GroupRoundRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupCommandService {

    private final GroupRepository groupRepository;
    private final GroupLocationRepository groupLocationRepository;
    private final GroupRoundRepository groupRoundRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupResponseRepository groupResponseRepository;

    /** 모임 생성: 그룹 + 기본 장소 + 최초 회차 저장 후, 생성자를 OWNER로 등록하고 최초 회차의 PENDING 응답을 만든다 */
    public GroupDetailResponse createGroup(Long userId, CreateGroupRequest request) {
        Group group = groupRepository.save(Group.builder()
                .name(request.name())
                .intro(request.intro())
                .remindOffer(request.remindOffer())
                .startDate(request.startDate())
                .startTime(request.startTime())
                .endDate(request.endDate())
                .build());

        GroupLocation location = groupLocationRepository.save(GroupLocation.builder()
                .group(group)
                .locationName(request.locationName())
                .locationAddress(request.locationAddress())
                .placeId(request.placeId())
                .build());

        GroupRound initialRound = groupRoundRepository.save(GroupRound.builder()
                .group(group)
                .roundDate(group.getStartDate())
                .roundTime(group.getStartTime())
                .roundLocationName(location.getLocationName())
                .roundLocationAddress(location.getLocationAddress())
                .roundChanged(false)
                .build());

        GroupMember owner = groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .userId(userId)
                .position(GroupMemberPosition.OWNER)
                .status(GroupMemberStatus.ACTIVE)
                .build());

        groupResponseRepository.save(GroupResponse.builder()
                .groupRound(initialRound)
                .member(owner)
                .attendanceStatus(AttendanceStatus.PENDING)
                .build());

        long memberCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), GroupMemberStatus.ACTIVE);
        return GroupDetailResponse.of(group, location, memberCount);
    }

    /** 회차 일정 변경: OWNER/CO_OWNER만 가능하며, 변경 시 해당 회차의 모든 응답을 재확인(RERESPONSE) 상태로 되돌린다 */
    public GroupRoundResponse rescheduleGroupRound(Long userId, Long roundId, RescheduleGroupRoundRequest request) {
        GroupRound round = groupRoundRepository.findById(roundId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_ROUND_NOT_FOUND));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(round.getGroup().getId(), userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
        validateManager(member);

        String newLocationName;
        if (request.locationName() != null) {
            newLocationName = request.locationName();
        } else {
            newLocationName = round.getRoundLocationName();
        }

        String newLocationAddress;
        if (request.locationAddress() != null) {
            newLocationAddress = request.locationAddress();
        } else {
            newLocationAddress = round.getRoundLocationAddress();
        }

        round.reschedule(request.roundDate(), request.roundTime(), newLocationName, newLocationAddress);

        List<GroupResponse> responses = groupResponseRepository.findByGroupRoundId(roundId);
        responses.forEach(GroupResponse::markRerespond);

        return GroupRoundResponse.from(round);
    }

    /** 참여자 본인의 출석 응답 제출/수정 (ATTEND, ABSENT만 허용) */
    public AttendanceResponse submitGroupResponse(Long userId, Long roundId, SubmitGroupResponseRequest request) {
        if (request.attendanceStatus() != AttendanceStatus.ATTEND && request.attendanceStatus() != AttendanceStatus.ABSENT) {
            throw new GeneralException(ErrorStatus.INVALID_ATTENDANCE_STATUS);
        }

        GroupRound round = groupRoundRepository.findById(roundId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_ROUND_NOT_FOUND));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(round.getGroup().getId(), userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_INACTIVE);
        }

        GroupResponse response = groupResponseRepository.findByGroupRoundIdAndMemberId(roundId, member.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_RESPONSE_NOT_FOUND));

        response.respond(request.attendanceStatus(), request.absenceReason());

        return AttendanceResponse.from(response);
    }

    /** 모임장 위임: ACTIVE OWNER만 가능, 대상은 본인이 아닌 ACTIVE 멤버. 기존 OWNER는 MEMBER로 내려간다 */
    public void updateGroupOwner(Long userId, Long groupId, Long newOwnerId) {
        if (!groupRepository.existsById(groupId)) {
            throw new GeneralException(ErrorStatus.GROUP_NOT_FOUND);
        }

        GroupMember owner = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .filter(m -> m.getStatus() == GroupMemberStatus.ACTIVE && m.getPosition() == GroupMemberPosition.OWNER)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_OWNER_FORBIDDEN));

        // 같은 모임에 위임 요청이 동시에 들어오는 경우는 잠그지 않는다. 문제 되면 모임 행 비관 잠금
        GroupMember newOwner = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId)
                .filter(m -> m.getStatus() == GroupMemberStatus.ACTIVE && !m.getUserId().equals(userId))
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_NEW_OWNER));

        owner.changePosition(GroupMemberPosition.MEMBER);
        newOwner.changePosition(GroupMemberPosition.OWNER);
    }

    /**
     * 회원 탈퇴 시 모든 모임에서 제거.
     * 다른 활동 멤버가 있는 OWNER 모임이 하나라도 있으면 위임이 먼저 필요하므로 목록과 함께 409.
     * 본인만 남은 OWNER 모임은 통째로 삭제하고, 나머지 모임은 멤버십과 참석 응답을 지운다.
     */
    public void deleteUserFromAllGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findAllByUserIdWithGroup(userId);

        List<OwnerGroupResponse> needDelegation = new ArrayList<>();
        List<Long> soloGroupIds = new ArrayList<>();
        List<Long> memberIds = new ArrayList<>();
        for (GroupMember membership : memberships) {
            boolean activeOwner = membership.getStatus() == GroupMemberStatus.ACTIVE
                    && membership.getPosition() == GroupMemberPosition.OWNER;
            if (!activeOwner) {
                memberIds.add(membership.getId());
                continue;
            }
            Long groupId = membership.getGroup().getId();
            if (groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE) > 1) {
                needDelegation.add(OwnerGroupResponse.from(membership.getGroup()));
            } else {
                soloGroupIds.add(groupId);
            }
        }

        if (!needDelegation.isEmpty()) {
            throw new GeneralException(ErrorStatus.USER_OWNER_GROUP_EXISTS, needDelegation);
        }

        soloGroupIds.forEach(this::deleteGroup);
        if (!memberIds.isEmpty()) {
            groupResponseRepository.deleteByMemberIdIn(memberIds);
            groupMemberRepository.deleteAllByIdInBatch(memberIds);
        }
    }

    /** 모임과 딸린 응답/회차/장소/멤버를 모두 삭제 (자식부터) */
    private void deleteGroup(Long groupId) {
        groupResponseRepository.deleteByGroupId(groupId);
        groupRoundRepository.deleteByGroupId(groupId);
        groupLocationRepository.deleteByGroupId(groupId);
        groupMemberRepository.deleteByGroupId(groupId);
        groupRepository.deleteById(groupId);
    }

    private void validateManager(GroupMember member) {
        if (!member.manager()) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_FORBIDDEN);
        }
    }
}
