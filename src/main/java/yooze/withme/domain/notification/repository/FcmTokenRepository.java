package yooze.withme.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.notification.entity.FcmToken;

import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    /** 유저의 모든 기기 토큰 조회 */
    List<FcmToken> findAllByUser(User user);

    /**
     * 기기 단위 원자적 upsert.
     * 동일 (user_id, device_id) 충돌 시 token만 갱신 — 경쟁 조건 없음.
     */
    @Modifying
    @Query(value = """
            INSERT INTO fcm_tokens (user_id, device_id, token, created_at, updated_at)
            VALUES (:userId, :deviceId, :token, now(), now())
            ON CONFLICT ON CONSTRAINT uq_fcm_tokens_user_device
            DO UPDATE SET token = EXCLUDED.token, updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("userId") Long userId,
                @Param("deviceId") String deviceId,
                @Param("token") String token);

    /**
     * 같은 토큰이 다른 유저에 등록돼 있으면 삭제.
     * 같은 기기에서 계정을 바꿔 로그인할 때 이전 계정의 토큰을 정리한다.
     */
    @Modifying
    @Query(value = "DELETE FROM fcm_tokens WHERE token = :token AND user_id != :userId",
            nativeQuery = true)
    void deleteStaleByToken(@Param("token") String token, @Param("userId") Long userId);

    /** 로그아웃 시 해당 기기의 토큰 삭제 */
    @Modifying
    @Query(value = "DELETE FROM fcm_tokens WHERE user_id = :userId AND device_id = :deviceId",
            nativeQuery = true)
    void deleteByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}
