package yooze.withme.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank(message = "기기 ID는 필수입니다.")
        String deviceId,
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String token
) {
}
