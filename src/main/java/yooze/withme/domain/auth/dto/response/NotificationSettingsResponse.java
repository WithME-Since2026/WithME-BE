package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.auth.entity.User;

public record NotificationSettingsResponse(
        boolean notifyAgree
) {

    public static NotificationSettingsResponse from(User user) {
        return new NotificationSettingsResponse(user.isNotifyAgree());
    }
}
