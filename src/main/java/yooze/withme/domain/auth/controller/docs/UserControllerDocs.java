package yooze.withme.domain.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.dto.request.UpdateNicknameRequest;
import yooze.withme.domain.auth.dto.request.UpdateNotificationSettingsRequest;
import yooze.withme.domain.auth.dto.request.WithdrawRequest;
import yooze.withme.domain.auth.dto.response.NotificationSettingsResponse;
import yooze.withme.domain.auth.dto.response.ProfileImageResponse;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.group.dto.response.AttendanceRateResponse;
import yooze.withme.domain.group.dto.response.MyGroupResponse;
import yooze.withme.domain.group.dto.response.WeeklyAttendanceResponse;

@Tag(name = "User", description = "유저 마이페이지(프로필/알림설정/참여율/내 모임 목록) API")
public interface UserControllerDocs {

    @Operation(summary = "이름 입력", description = "회원가입한 사용자의 닉네임을 입력한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이름 입력 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me/profile")
    ResponseEntity<ApiResponse<ProfileResponse>> patchUserProfile(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody UpdateNicknameRequest request
    );

    @Operation(summary = "프로필 조회", description = "내 프로필(닉네임, 프로필 이미지)을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/profile")
    ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "알림 설정 조회", description = "모임 리마인드 / 할 일 마감 / 새 모임 초대 알림 수신 여부를 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/notifications")
    ResponseEntity<ApiResponse<NotificationSettingsResponse>> getUserNotificationSettings(
            @Parameter(hidden = true) UserDetails userDetails
    );

    @Operation(summary = "알림 설정 수정", description = "모임 리마인드 / 할 일 마감 / 새 모임 초대 알림 수신 여부를 각각 변경한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me/notifications")
    ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateUserNotificationSettings(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody UpdateNotificationSettingsRequest request
    );

    @Operation(summary = "참여율 조회", description = "내가 참여한 모임 회차들의 참석 응답 중 ATTEND 비율을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여율 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/attendance")
    ResponseEntity<ApiResponse<AttendanceRateResponse>> getUserAttendanceRate(
            @Parameter(hidden = true) UserDetails userDetails
    );

    @Operation(summary = "참여 모임 목록 조회", description = "내가 활동 중(ACTIVE)인 모임 목록을 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여 모임 목록 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/groups")
    ResponseEntity<ApiResponse<List<MyGroupResponse>>> getUserGroups(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "주차별 참석률 조회", description = "최근 weeks주(이번 주 포함, 월요일 시작, KST)의 주별 참석률을 오래된 주부터 조회한다. 일정이 없던 주는 0으로 채운다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주차별 참석률 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "weeks 는 1~52")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/attendance/weekly")
    ResponseEntity<ApiResponse<List<WeeklyAttendanceResponse>>> getUserWeeklyAttendance(
            @Parameter(hidden = true) UserDetails userDetails,
            @RequestParam(defaultValue = "6") int weeks
    );

    @Operation(summary = "프로필 이미지 변경", description = "jpg/png, 최대 5MB 이미지로 프로필 이미지를 변경한다. 기존 이미지는 삭제된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 이미지 변경 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 이미지 형식")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "5MB 초과")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<ProfileImageResponse>> updateProfileImage(
            @Parameter(hidden = true) UserDetails userDetails,
            @RequestPart("image") MultipartFile image
    );

    @Operation(summary = "프로필 이미지 삭제", description = "프로필 이미지를 기본 이미지(null)로 되돌린다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 이미지 삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me/profile-image")
    ResponseEntity<ApiResponse<Void>> deleteProfileImage(@Parameter(hidden = true) UserDetails userDetails);

    @Operation(summary = "회원 탈퇴", description = "소프트 삭제한다. 다른 멤버가 있는 모임장 모임이 남아 있으면 409와 함께 위임이 필요한 모임 목록을 data 로 반환한다. 본인만 있는 모임은 함께 삭제된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "모임장 위임이 필요한 모임 존재 (USER_409)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(hidden = true) UserDetails userDetails,
            @RequestBody(required = false) WithdrawRequest request
    );
}
