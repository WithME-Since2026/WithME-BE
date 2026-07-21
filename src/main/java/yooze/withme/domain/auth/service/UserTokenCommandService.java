package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserToken;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.enums.TokenStatus;
import yooze.withme.domain.auth.repository.UserTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserTokenCommandService {

    private final UserTokenRepository userTokenRepository;

    /** 토큰 발급 (기존 토큰 있으면 갱신, 없으면 신규 생성) */
    public UserToken issueToken(User user, String withmeRefresh, ProviderType provider, LocalDateTime expiredAt) {
        Optional<UserToken> existingToken =
                userTokenRepository.findByUserAndProviderAndStatus(user, provider, TokenStatus.ACTIVE);

        if (existingToken.isPresent()) {
            existingToken.get().updateWithmeRefresh(withmeRefresh, expiredAt);
            return existingToken.get();
        }

        UserToken userToken = UserToken.builder()
                .user(user)
                .withmeRefresh(withmeRefresh)
                .provider(provider)
                .expiredAt(expiredAt)
                .status(TokenStatus.ACTIVE)
                .build();

        return userTokenRepository.save(userToken);
    }

    /** 토큰 비활성화 (로그아웃) */
    public void revokeToken(User user, ProviderType provider) {
        userTokenRepository.findByUserAndProviderAndStatus(user, provider, TokenStatus.ACTIVE)
                .ifPresent(UserToken::deactivate);
    }
}
