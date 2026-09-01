package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.auth.entity.User;

public record NotificationSettingsResponse(
        boolean notifyGroupRemind,
        boolean notifyTodoDeadline,
        boolean notifyGroupInvite
) {

    public static NotificationSettingsResponse from(User user) {
        return new NotificationSettingsResponse(
                user.isNotifyGroupRemind(),
                user.isNotifyTodoDeadline(),
                user.isNotifyGroupInvite()
        );
    }
}
