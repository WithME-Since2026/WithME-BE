package yooze.withme.domain.group.service;

import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;

public interface GroupCommandService {

    GroupDetailResponse createGroup(Long userId, CreateGroupRequest request);

    GroupRoundResponse rescheduleGroupRound(Long userId, Long roundId, RescheduleGroupRoundRequest request);

    AttendanceResponse submitGroupResponse(Long userId, Long roundId, SubmitGroupResponseRequest request);
}
