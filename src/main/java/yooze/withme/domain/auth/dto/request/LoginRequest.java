package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        String localId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {
}
