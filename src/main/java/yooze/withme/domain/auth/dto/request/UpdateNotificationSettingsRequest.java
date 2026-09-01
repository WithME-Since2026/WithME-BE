package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequest(

        @NotNull(message = "모임 리마인드 알림 여부는 필수입니다.")
        Boolean notifyGroupRemind,

        @NotNull(message = "할 일 마감 알림 여부는 필수입니다.")
        Boolean notifyTodoDeadline,

        @NotNull(message = "새 모임 초대 알림 여부는 필수입니다.")
        Boolean notifyGroupInvite

) {
}
