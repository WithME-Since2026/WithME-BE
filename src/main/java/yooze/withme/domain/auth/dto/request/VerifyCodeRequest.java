package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증코드는 필수입니다.")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증코드는 숫자 6자리이어야 합니다.")
        String code

) {
}
