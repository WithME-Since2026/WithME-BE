package yooze.withme.domain.group.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.error.ErrorStatus;
import yooze.withme.domain.group.converter.GroupConverter;
import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.entity.Group;
import yooze.withme.domain.group.entity.GroupLocation;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.group.enums.GroupMemberStatus;
import yooze.withme.domain.group.repository.GroupLocationRepository;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;
import yooze.withme.domain.group.repository.GroupRoundRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupCommandServiceImpl implements GroupCommandService {

    private final GroupRepository groupRepository;
    private final GroupLocationRepository groupLocationRepository;
    private final GroupRoundRepository groupRoundRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupResponseRepository groupResponseRepository;

    /** 모임 생성: 그룹 + 기본 장소 + 최초 회차 저장 후, 생성자를 OWNER로 등록하고 최초 회차의 PENDING 응답을 만든다 */
    @Override
    public GroupDetailResponse createGroup(Long userId, CreateGroupRequest request) {
        Group group = groupRepository.save(GroupConverter.toGroupEntity(request));
        GroupLocation location = groupLocationRepository.save(GroupConverter.toGroupLocationEntity(group, request));
        GroupRound initialRound = groupRoundRepository.save(GroupConverter.toInitialRoundEntity(group, location));

        GroupMember owner = groupMemberRepository.save(GroupConverter.toOwnerMemberEntity(group, userId));
        groupResponseRepository.save(GroupConverter.toPendingResponseEntity(initialRound, owner));

        long memberCount = groupMemberRepository.countByGroupIdAndStatus(group.getId(), GroupMemberStatus.ACTIVE);
        return GroupDetailResponse.of(group, location, memberCount);
    }

    /** 회차 일정 변경: OWNER/CO_OWNER만 가능하며, 변경 시 해당 회차의 모든 응답을 재확인(RERESPONSE) 상태로 되돌린다 */
    @Override
    public GroupRoundResponse rescheduleGroupRound(Long userId, Long roundId, RescheduleGroupRoundRequest request) {
        GroupRound round = groupRoundRepository.findById(roundId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_ROUND_NOT_FOUND));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(round.getGroup().getId(), userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
        validateManager(member);

        String newLocationName = request.locationName() != null ? request.locationName() : round.getRoundLocationName();
        String newLocationAddress = request.locationAddress() != null ? request.locationAddress() : round.getRoundLocationAddress();
        round.reschedule(request.roundDate(), request.roundTime(), newLocationName, newLocationAddress);

        List<GroupResponse> responses = groupResponseRepository.findByGroupRoundId(roundId);
        responses.forEach(GroupResponse::markRerespond);

        return GroupRoundResponse.from(round);
    }

    /** 참여자 본인의 출석 응답 제출/수정 (ATTEND, ABSENT만 허용) */
    @Override
    public AttendanceResponse submitGroupResponse(Long userId, Long roundId, SubmitGroupResponseRequest request) {
        if (request.attendanceStatus() != AttendanceStatus.ATTEND && request.attendanceStatus() != AttendanceStatus.ABSENT) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        GroupRound round = groupRoundRepository.findById(roundId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_ROUND_NOT_FOUND));

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(round.getGroup().getId(), userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));

        GroupResponse response = groupResponseRepository.findByGroupRoundIdAndMemberId(roundId, member.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_RESPONSE_NOT_FOUND));

        response.respond(request.attendanceStatus(), request.absenceReason());

        return AttendanceResponse.from(response);
    }

    private void validateManager(GroupMember member) {
        if (!member.isManager()) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_FORBIDDEN);
        }
    }
}
