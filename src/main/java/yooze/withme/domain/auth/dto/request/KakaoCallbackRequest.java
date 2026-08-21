package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoCallbackRequest(
        @NotBlank(message = "카카오 인가 코드는 필수입니다.")
        String code,
        @NotBlank(message = "state 값은 필수입니다.")
        String state
) {
}
