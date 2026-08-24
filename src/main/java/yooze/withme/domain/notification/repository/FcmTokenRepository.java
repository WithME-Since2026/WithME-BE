package yooze.withme.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.notification.entity.FcmToken;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    /** 유저의 모든 기기 토큰 조회 */
    List<FcmToken> findAllByUser(User user);

    /** 기기 단위 upsert용 */
    Optional<FcmToken> findByUserAndDeviceId(User user, String deviceId);
}
