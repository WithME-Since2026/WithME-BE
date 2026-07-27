package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private final StringRedisTemplate redisTemplate;

    /** 리프레시 토큰 저장 (TTL 설정) */
    public void save(String id, String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(id, token, ttlSeconds, TimeUnit.SECONDS);
    }

    /** 리프레시 토큰 조회 */
    public Optional<String> findById(String id) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(id));
    }

    /** 리프레시 토큰 삭제 */
    public void deleteById(String id) {
        redisTemplate.delete(id);
    }
}
