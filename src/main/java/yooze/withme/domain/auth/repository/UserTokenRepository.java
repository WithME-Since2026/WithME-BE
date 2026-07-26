package yooze.withme.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserToken;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.enums.TokenStatus;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    /** 동시 발급 시 중복 행이 생길 수 있으므로, 최신 1건을 명시적으로 선택 */
    Optional<UserToken> findFirstByUserAndProviderAndStatusOrderByTokenIdDesc(User user, ProviderType provider, TokenStatus status);
}
