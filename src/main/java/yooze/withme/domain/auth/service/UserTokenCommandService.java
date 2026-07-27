package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.RefreshTokenRedisRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTokenCommandService {

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    /** 리프레시 토큰 발급 (기존 토큰 있으면 덮어쓰기, TTL 자동 관리) */
    public void issueToken(User user, String withmeRefresh, ProviderType provider, LocalDateTime expiredAt) {
        long ttl = ChronoUnit.SECONDS.between(LocalDateTime.now(), expiredAt);
        String id = user.getUserId() + ":" + provider.name();

        refreshTokenRedisRepository.save(id, withmeRefresh, ttl);
        log.info("리프레시 토큰 저장 - userId: {}, provider: {}, ttl: {}s", user.getUserId(), provider, ttl);
    }

    /** 리프레시 토큰 삭제 (로그아웃) */
    public void revokeToken(User user, ProviderType provider) {
        String id = user.getUserId() + ":" + provider.name();
        refreshTokenRedisRepository.deleteById(id);
        log.info("리프레시 토큰 삭제 - userId: {}, provider: {}", user.getUserId(), provider);
    }
}
