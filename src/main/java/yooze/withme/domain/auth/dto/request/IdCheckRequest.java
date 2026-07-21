package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IdCheckRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "아이디는 6~20자 영문, 숫자만 가능합니다.")
        String localId

) {
}
