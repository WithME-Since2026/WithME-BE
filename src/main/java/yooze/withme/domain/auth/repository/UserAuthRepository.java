package yooze.withme.domain.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    Optional<UserAuth> findByLocalIdAndProvider(String localId, ProviderType provider);

    boolean existsByLocalIdAndProvider(String localId, ProviderType provider);

    Optional<UserAuth> findByUserAndProvider(User user, ProviderType provider);

    Optional<UserAuth> findByUser_EmailAndProvider(String email, ProviderType provider);

    Optional<UserAuth> findByProviderAndProviderUserId(ProviderType provider, Long providerUserId);
}
