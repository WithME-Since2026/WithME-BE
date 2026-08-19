package yooze.withme.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.notification.entity.FcmToken;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByUser(User user);
}
