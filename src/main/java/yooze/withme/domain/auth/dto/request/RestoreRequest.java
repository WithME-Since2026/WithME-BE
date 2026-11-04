package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RestoreRequest(
        @NotBlank(message = "복구 토큰은 필수입니다.")
        String restoreToken
) {
}
