package yooze.withme.domain.auth.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.auth.dto.request.UpdateNicknameRequest;
import yooze.withme.domain.auth.dto.request.UpdateNotificationSettingsRequest;
import yooze.withme.domain.auth.dto.response.AttendanceRateResponse;
import yooze.withme.domain.auth.dto.response.MyGroupResponse;
import yooze.withme.domain.auth.dto.response.NotificationSettingsResponse;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.auth.controller.docs.UserControllerDocs;
import yooze.withme.domain.auth.service.UserCommandService;
import yooze.withme.domain.auth.service.UserQueryService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> patchUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        ProfileResponse profileResponse = userCommandService.updateNickname(userId, request.nickname());
        return ApiResponse.success(SuccessStatus.SUCCESS_200, profileResponse);
    }

    @Override
    public ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        ProfileResponse response = userQueryService.getUserProfile(userId);
        return ApiResponse.success(SuccessStatus.GET_PROFILE_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getUserNotificationSettings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        NotificationSettingsResponse response = userQueryService.getUserNotificationSettings(userId);
        return ApiResponse.success(SuccessStatus.GET_NOTIFICATION_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateUserNotificationSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNotificationSettingsRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        NotificationSettingsResponse response = userCommandService.updateNotificationSettings(userId, request.notifyAgree());
        return ApiResponse.success(SuccessStatus.UPDATE_NOTIFICATION_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<AttendanceRateResponse>> getUserAttendanceRate(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        AttendanceRateResponse response = userQueryService.getUserAttendanceRate(userId);
        return ApiResponse.success(SuccessStatus.GET_ATTENDANCE_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<MyGroupResponse>>> getUserGroups(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<MyGroupResponse> response = userQueryService.getUserGroups(userId);
        return ApiResponse.success(SuccessStatus.GET_MY_GROUPS_SUCCESS, response);
    }
}
