package yooze.withme.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.controller.docs.UserWithdrawalControllerDocs;
import yooze.withme.domain.auth.dto.request.WithdrawUserRequest;
import yooze.withme.domain.auth.service.UserWithdrawalService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserWithdrawalController implements UserWithdrawalControllerDocs {

    private final UserWithdrawalService userWithdrawalService;

    /** 인증된 본인 계정을 탈퇴한다. 사용자 ID는 요청 본문에서 받지 않는다. */
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawUserRequest request
    ) {
        userWithdrawalService.withdrawUser(Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.noContent().build();
    }
}
