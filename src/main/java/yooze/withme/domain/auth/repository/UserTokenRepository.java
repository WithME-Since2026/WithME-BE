package yooze.withme.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserToken;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.enums.TokenStatus;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByUserAndProviderAndStatus(User user, ProviderType provider, TokenStatus status);
}
