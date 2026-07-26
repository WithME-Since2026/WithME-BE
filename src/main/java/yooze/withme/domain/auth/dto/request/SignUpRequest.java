package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "아이디는 6~20자 영문, 숫자만 가능합니다.")
        String localId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        @NotNull(message = "서비스 이용약관 동의는 필수입니다.")
        @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
        Boolean serviceTermsAgreed,

        @NotNull(message = "개인정보 처리방침 동의는 필수입니다.")
        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
        Boolean privacyPolicyAgreed,

        Boolean marketingAgreed

) {
}
