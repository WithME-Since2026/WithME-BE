package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import yooze.withme.domain.auth.enums.ProviderType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 탈퇴 계정 복구용 일회성 토큰 저장소. 로그인 인증에 성공한 탈퇴 사용자에게만 발급한다 */
@Repository
@RequiredArgsConstructor
public class RestoreTokenRedisRepository {

    private static final String PREFIX = "auth:restore:";
    private static final long TOKEN_TTL_SECONDS = 600; // 10분

    private final StringRedisTemplate redisTemplate;

    /** 토큰 발급 */
    public String issue(Long userId, ProviderType provider) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + token, userId + ":" + provider.name(), TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    /** 토큰을 조회하면서 삭제한다. 동시 요청이 와도 한 번만 성공한다 */
    public Optional<RestoreTarget> consume(String token) {
        String value = redisTemplate.opsForValue().getAndDelete(PREFIX + token);
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split(":");
        return Optional.of(new RestoreTarget(Long.valueOf(parts[0]), ProviderType.valueOf(parts[1])));
    }

    public record RestoreTarget(Long userId, ProviderType provider) {}
}
