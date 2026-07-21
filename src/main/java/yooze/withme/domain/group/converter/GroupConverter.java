package yooze.withme.domain.group.converter;

import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.entity.Group;
import yooze.withme.domain.group.entity.GroupLocation;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.group.enums.GroupMemberPosition;
import yooze.withme.domain.group.enums.GroupMemberStatus;

public class GroupConverter {

    private GroupConverter() {
    }

    public static Group toGroupEntity(CreateGroupRequest request) {
        return Group.builder()
                .name(request.name())
                .intro(request.intro())
                .remindOffer(request.remindOffer())
                .startDate(request.startDate())
                .startTime(request.startTime())
                .endDate(request.endDate())
                .build();
    }

    public static GroupLocation toGroupLocationEntity(Group group, CreateGroupRequest request) {
        return GroupLocation.builder()
                .group(group)
                .locationName(request.locationName())
                .locationAddress(request.locationAddress())
                .placeId(request.placeId())
                .build();
    }

    /** 모임 생성 시 그룹의 시작 일시/기본 장소를 그대로 물려받는 최초 회차(1회차) */
    public static GroupRound toInitialRoundEntity(Group group, GroupLocation location) {
        return GroupRound.builder()
                .group(group)
                .roundDate(group.getStartDate())
                .roundTime(group.getStartTime())
                .roundLocationName(location.getLocationName())
                .roundLocationAddress(location.getLocationAddress())
                .roundChanged(false)
                .build();
    }

    public static GroupMember toOwnerMemberEntity(Group group, Long userId) {
        return GroupMember.builder()
                .group(group)
                .userId(userId)
                .position(GroupMemberPosition.OWNER)
                .status(GroupMemberStatus.ACTIVE)
                .build();
    }

    public static GroupResponse toPendingResponseEntity(GroupRound round, GroupMember member) {
        return GroupResponse.builder()
                .groupRound(round)
                .member(member)
                .attendanceStatus(AttendanceStatus.PENDING)
                .build();
    }
}
