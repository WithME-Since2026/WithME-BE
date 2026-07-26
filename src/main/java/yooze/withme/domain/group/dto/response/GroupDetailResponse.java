package yooze.withme.domain.group.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import yooze.withme.domain.group.entity.Group;
import yooze.withme.domain.group.entity.GroupLocation;

public record GroupDetailResponse(
        Long groupId,
        String name,
        String intro,
        boolean remindOffer,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        String locationName,
        String locationAddress,
        long memberCount
) {

    public static GroupDetailResponse of(Group group, GroupLocation location, long memberCount) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getIntro(),
                group.isRemindOffer(),
                group.getStartDate(),
                group.getStartTime(),
                group.getEndDate(),
                location.getLocationName(),
                location.getLocationAddress(),
                memberCount
        );
    }
}
