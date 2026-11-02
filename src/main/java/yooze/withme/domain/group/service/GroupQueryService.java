package yooze.withme.domain.group.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.group.dto.response.AttendanceRateResponse;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.dto.response.MyGroupResponse;
import yooze.withme.domain.group.dto.response.WeeklyAttendanceResponse;
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
@Transactional(readOnly = true)
public class GroupQueryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

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

    /** 특정 유저의 참여율 조회 - 현재 활동 중(ACTIVE)인 모임에서 결정된(ATTEND/ABSENT) 응답 중 ATTEND 비율 */
    public AttendanceRateResponse getUserAttendanceRate(Long userId) {
        long attendCount = groupResponseRepository.countAttendance(userId, GroupMemberStatus.ACTIVE, AttendanceStatus.ATTEND);
        long absentCount = groupResponseRepository.countAttendance(userId, GroupMemberStatus.ACTIVE, AttendanceStatus.ABSENT);
        return AttendanceRateResponse.of(attendCount, absentCount);
    }

    /** 최근 weeks 주(이번 주 포함, 월요일 시작, KST)의 주별 참석률. 오래된 주부터, 빈 주도 0으로 채운다 */
    public List<WeeklyAttendanceResponse> getUserWeeklyAttendance(Long userId, int weeks) {
        LocalDate thisMonday = LocalDate.now(KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate from = thisMonday.minusWeeks(weeks - 1L);
        List<GroupResponse> responses = groupResponseRepository.findByUserIdAndRoundDateBetween(
                userId, GroupMemberStatus.ACTIVE, from, thisMonday.plusDays(6));
        return groupByWeek(from, weeks, responses);
    }

    /** 기존 참여율과 같은 기준 — ATTEND/ABSENT만 센다 */
    static List<WeeklyAttendanceResponse> groupByWeek(LocalDate from, int weeks, List<GroupResponse> responses) {
        long[] attend = new long[weeks];
        long[] absent = new long[weeks];
        for (GroupResponse response : responses) {
            int week = (int) ChronoUnit.WEEKS.between(from, response.getGroupRound().getRoundDate());
            if (response.getAttendanceStatus() == AttendanceStatus.ATTEND) {
                attend[week]++;
            } else if (response.getAttendanceStatus() == AttendanceStatus.ABSENT) {
                absent[week]++;
            }
        }

        List<WeeklyAttendanceResponse> result = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            result.add(WeeklyAttendanceResponse.of(from.plusWeeks(i), attend[i], absent[i]));
        }
        return result;
    }

    /** 특정 유저가 활동 중(ACTIVE)인 모임 목록 조회 */
    public List<MyGroupResponse> getUserGroups(Long userId) {
        return groupMemberRepository.findByUserIdAndStatus(userId, GroupMemberStatus.ACTIVE).stream()
                .map(MyGroupResponse::from)
                .toList();
    }
}
