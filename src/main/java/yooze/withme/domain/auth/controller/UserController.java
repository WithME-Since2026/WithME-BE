package yooze.withme.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.success.SuccessStatus;
import yooze.withme.domain.auth.dto.request.UpdateNicknameRequest;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.auth.service.UserCommandService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> patchUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        ProfileResponse profileResponse = userCommandService.updateNickname(userId, request.nickname());
        return ApiResponse.success(SuccessStatus.SUCCESS_200, profileResponse);
    }
}
