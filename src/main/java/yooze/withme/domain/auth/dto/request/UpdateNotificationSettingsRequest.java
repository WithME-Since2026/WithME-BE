package yooze.withme.domain.auth.dto.request;

public record UpdateNotificationSettingsRequest(

        Boolean notifyGroupRemind,
        Boolean notifyTodoDeadline,
        Boolean notifyGroupInvite

) {
}
