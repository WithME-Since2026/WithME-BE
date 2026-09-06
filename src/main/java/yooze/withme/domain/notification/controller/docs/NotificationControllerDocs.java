package yooze.withme.domain.notification.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.notification.dto.request.FcmTokenRequest;
import yooze.withme.domain.notification.dto.response.NotificationResponse;

import java.util.List;
import java.util.Map;

@Tag(name = "알림", description = "알림 목록 조회 / 읽음 처리 / FCM 토큰 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationControllerDocs {

    @Operation(summary = "알림 목록 조회", description = "내 알림 목록을 최신순으로 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @GetMapping
    ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @Parameter(hidden = true) UserDetails userDetails
    );

    @Operation(summary = "안읽은 알림 수 조회", description = "읽지 않은 알림 수를 반환한다. 뱃지 폴링용으로 사용한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "안읽은 알림 수 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @GetMapping("/unread-count")
    ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @Parameter(hidden = true) UserDetails userDetails
    );

    @Operation(summary = "단건 읽음 처리", description = "특정 알림 하나를 읽음 처리한다. 본인 알림만 처리된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @PatchMapping("/{notificationId}/read")
    ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(hidden = true) UserDetails userDetails,
            @Parameter(description = "알림 ID", required = true) @PathVariable Long notificationId
    );

    @Operation(summary = "전체 읽음 처리", description = "내 모든 알림을 읽음 처리한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @PatchMapping("/read-all")
    ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Parameter(hidden = true) UserDetails userDetails
    );

    @Operation(
            summary = "FCM 토큰 등록/갱신",
            description = "기기의 FCM 토큰을 등록하거나 갱신한다. "
                    + "로그인 직후 및 FCM 토큰이 갱신될 때마다 호출해야 한다. "
                    + "같은 deviceId로 요청하면 토큰을 덮어쓰고, 새 deviceId면 새 기기로 등록된다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FCM 토큰 등록/갱신 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @PostMapping("/fcm-token")
    ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody FcmTokenRequest request
    );
}
