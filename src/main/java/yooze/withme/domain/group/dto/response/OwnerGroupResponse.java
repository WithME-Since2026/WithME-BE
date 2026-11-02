package yooze.withme.domain.group.dto.response;

import yooze.withme.domain.group.entity.Group;

/** 탈퇴 전 모임장 위임이 필요한 모임 */
public record OwnerGroupResponse(
        Long groupId,
        String groupName
) {

    public static OwnerGroupResponse from(Group group) {
        return new OwnerGroupResponse(group.getId(), group.getName());
    }
}
