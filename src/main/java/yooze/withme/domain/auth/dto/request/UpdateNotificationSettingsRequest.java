package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequest(

        @NotNull(message = "알림 수신 동의 여부는 필수입니다.")
        Boolean notifyAgree

) {
}
