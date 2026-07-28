package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.enums.GroupMemberPosition;

public record MyGroupResponse(
        Long groupId,
        String name,
        GroupMemberPosition position
) {

    public static MyGroupResponse from(GroupMember member) {
        return new MyGroupResponse(member.getGroup().getId(), member.getGroup().getName(), member.getPosition());
    }
}
