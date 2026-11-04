package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.common.storage.S3ImageStorage;
import yooze.withme.domain.auth.client.KakaoAuthClient;
import yooze.withme.domain.auth.dto.response.NotificationSettingsResponse;
import yooze.withme.domain.auth.dto.response.ProfileImageResponse;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.auth.dto.response.RestoreTokenResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.RestoreTokenRedisRepository;
import yooze.withme.domain.auth.repository.UserAuthRepository;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.notification.service.NotificationCommandService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandService {

    private static final long MAX_PROFILE_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final UserRepository userRepository;
    /**
     * 카카오 신규 사용자 등록(User + UserAuth)을 원자적으로 처리하기 위해 함께 보유.
     * 두 저장이 단일 트랜잭션 안에 있어야 하므로 별도 서비스 호출 대신 직접 참조한다.
     */
    private final UserAuthRepository userAuthRepository;
    private final UserTokenCommandService userTokenCommandService;
    private final GroupCommandService groupCommandService;
    private final NotificationCommandService notificationCommandService;
    private final KakaoAuthClient kakaoAuthClient;
    private final S3ImageStorage s3ImageStorage;
    private final RestoreTokenRedisRepository restoreTokenRedisRepository;

    /** 신규 사용자 생성 */
    public User registerUser(String nickname, String email) {
        User user = User.builder()
                .nickname(nickname)
                .email(email)
                .kakaoSync(false)
                .notifyGroupRemind(false)
                .notifyTodoDeadline(false)
                .notifyGroupInvite(false)
                .build();

        return userRepository.save(user);
    }

    /**
     * 카카오 로그인 — 기존 연동 사용자면 조회, 신규면 User + UserAuth 함께 등록.
     * 동시 요청으로 unique 충돌 시 DataIntegrityViolationException을 throw하며 트랜잭션이 롤백된다.
     * 호출자(AuthCommandService)가 catch하여 새 트랜잭션으로 findKakaoUser()를 호출한다.
     *
     * @return (user, newUser)
     */
    public KakaoUserResult findOrRegisterKakaoUser(Long kakaoId, String email, String nickname) {
        // 기존 카카오 연동 계정 확인
        Optional<UserAuth> existing = userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId);
        if (existing.isPresent()) {
            User user = existing.get().getUser();
            if (user.getDeletedAt() != null) {
                // 바로 복구하지 않는다. 사용자 의사를 확인한 뒤 복구 API 로 복구한다
                String restoreToken = restoreTokenRedisRepository.issue(user.getUserId(), ProviderType.KAKAO);
                throw new GeneralException(ErrorStatus.DELETED_USER, new RestoreTokenResponse(restoreToken));
            }
            return new KakaoUserResult(user, false);
        }

        // 신규 등록 시도 — 동시 요청으로 unique 충돌 시 DataIntegrityViolationException을 그대로 throw.
        // 이 트랜잭션은 롤백되고, 호출자(AuthCommandService)가 새 트랜잭션으로 재조회한다.
        User user = userRepository.save(User.builder()
                .nickname(nickname)
                .email(email)
                .kakaoSync(true)
                .notifyGroupRemind(false)
                .notifyTodoDeadline(false)
                .notifyGroupInvite(false)
                .build());

        userAuthRepository.save(UserAuth.builder()
                .user(user)
                .provider(ProviderType.KAKAO)
                .providerUserId(kakaoId)
                .build());

        return new KakaoUserResult(user, true);
    }

    /**
     * 카카오 계정 재조회 — 동시 INSERT 충돌 후 롤백된 트랜잭션과 독립된 새 트랜잭션에서 실행된다.
     * 조회 결과가 없으면 다른 계정에 이미 사용 중인 이메일이므로 예외를 던진다.
     */
    @Transactional
    public KakaoUserResult findKakaoUser(Long kakaoId) {
        return userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId)
                .filter(auth -> auth.getUser().getDeletedAt() == null)
                .map(auth -> new KakaoUserResult(auth.getUser(), false))
                .orElseThrow(() -> new GeneralException(ErrorStatus.EMAIL_ALREADY_REGISTERED));
    }

    /** 닉네임(이름) 변경 */
    public ProfileResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNickname(nickname);
        return ProfileResponse.from(user);
    }

    /** 알림 설정 변경 — null 필드는 기존 값을 유지한다 (하위 호환) */
    public NotificationSettingsResponse updateNotificationSettings(
            Long userId, Boolean notifyGroupRemind, Boolean notifyTodoDeadline, Boolean notifyGroupInvite) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNotificationSettings(
                notifyGroupRemind  != null ? notifyGroupRemind  : user.isNotifyGroupRemind(),
                notifyTodoDeadline != null ? notifyTodoDeadline : user.isNotifyTodoDeadline(),
                notifyGroupInvite  != null ? notifyGroupInvite  : user.isNotifyGroupInvite()
        );
        return NotificationSettingsResponse.from(user);
    }

    /**
     * 회원 탈퇴 (소프트 삭제).
     * 위임이 필요한 모임이 남아 있으면 GroupCommandService 가 409 를 던져 전체가 롤백된다.
     * 카카오 unlink 는 외부 호출이라 커밋 이후에 실행한다.
     */
    public void deleteUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        groupCommandService.deleteUserFromAllGroups(userId);

        List<UserAuth> auths = userAuthRepository.findAllByUser(user);
        auths.forEach(auth -> userTokenCommandService.revokeToken(user, auth.getProvider()));
        notificationCommandService.deleteAllFcmTokens(userId);
        user.delete();

        auths.stream()
                .filter(auth -> auth.getProvider() == ProviderType.KAKAO)
                .map(UserAuth::getProviderUserId)
                .forEach(kakaoId -> afterCommit(() -> kakaoAuthClient.unlink(kakaoId)));

        // 탈퇴 사유는 로그로만 남긴다. 통계가 필요해지면 테이블로
        log.info("회원 탈퇴 - userId: {}, reason: {}", userId, reason);
    }

    /** 프로필 이미지 변경 — jpg/png 만, 최대 5MB. 기존 이미지는 커밋 후, 새 이미지는 롤백 시 삭제 */
    public ProfileImageResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        byte[] bytes = readProfileImage(image);
        String extension = imageExtension(bytes);
        String contentType = extension.equals("png") ? "image/png" : "image/jpeg";
        String url = s3ImageStorage.upload(
                "profile/" + userId + "/" + UUID.randomUUID() + "." + extension, bytes, contentType);
        afterRollback(() -> s3ImageStorage.delete(url));

        replaceProfileImg(user, url);
        return new ProfileImageResponse(url);
    }

    /** 프로필 이미지 삭제 — 기본 이미지(null)로 되돌린다 */
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        replaceProfileImg(user, null);
    }

    /** 프로필 이미지 URL 교체 후 기존 이미지는 커밋 이후에 삭제 */
    private void replaceProfileImg(User user, String url) {
        String oldUrl = user.getProfileImg();
        user.updateProfileImg(url);
        afterCommit(() -> s3ImageStorage.delete(oldUrl));
    }

    /** 업로드 파일의 존재 여부와 용량을 검증하고 내용을 읽는다 */
    private byte[] readProfileImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_IMAGE_TYPE);
        }
        if (image.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new GeneralException(ErrorStatus.IMAGE_TOO_LARGE);
        }
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    /** Content-Type 은 클라이언트가 정하므로 파일 시그니처로 형식을 판별한다 */
    static String imageExtension(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length >= PNG_SIGNATURE.length && Arrays.equals(Arrays.copyOf(bytes, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            return "png";
        }
        throw new GeneralException(ErrorStatus.INVALID_IMAGE_TYPE);
    }

    /** 현재 트랜잭션이 커밋된 뒤에 실행할 작업 등록 */
    private void afterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    /** 현재 트랜잭션이 롤백된 뒤에 실행할 작업 등록. 커밋 여부를 알 수 없는 STATUS_UNKNOWN 에는 실행하지 않는다 */
    private void afterRollback(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    task.run();
                }
            }
        });
    }

    public record KakaoUserResult(User user, boolean newUser) {}
}
