package yooze.withme.domain.group.service;

import java.util.List;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;

public interface GroupQueryService {

    GroupDetailResponse getGroupDetail(Long groupId);

    List<GroupRoundResponse> getGroupRounds(Long groupId);

    List<AttendanceResponse> getGroupResponses(Long userId, Long roundId);
}
