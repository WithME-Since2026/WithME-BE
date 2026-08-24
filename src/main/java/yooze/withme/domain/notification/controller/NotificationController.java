package yooze.withme.domain.notification.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.service.UserQueryService;
import yooze.withme.domain.notification.dto.request.FcmTokenRequest;
import yooze.withme.domain.notification.dto.response.NotificationResponse;
import yooze.withme.domain.notification.service.NotificationCommandService;
import yooze.withme.domain.notification.service.NotificationQueryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final UserQueryService userQueryService;

    /** 내 알림 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<NotificationResponse> response = notificationQueryService.getNotifications(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return ApiResponse.success(SuccessStatus.GET_NOTIFICATIONS_SUCCESS, response);
    }

    /** 안읽은 알림 수 (폴링용 뱃지) */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        long count = notificationQueryService.getUnreadCount(userId);
        return ApiResponse.success(SuccessStatus.GET_UNREAD_COUNT_SUCCESS, Map.of("count", count));
    }

    /** 단건 읽음 처리 */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId
    ) {
        notificationCommandService.markAsRead(notificationId);
        return ApiResponse.success(SuccessStatus.MARK_AS_READ_SUCCESS);
    }

    /** 전체 읽음 처리 */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationCommandService.markAllAsRead(userId);
        return ApiResponse.success(SuccessStatus.MARK_AS_READ_SUCCESS);
    }

    /** FCM 토큰 등록/갱신 */
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        User user = userQueryService.getUserByUserId(userId);
        notificationCommandService.registerFcmToken(user, request.deviceId(), request.token());
        return ApiResponse.success(SuccessStatus.REGISTER_FCM_TOKEN_SUCCESS);
    }
}
