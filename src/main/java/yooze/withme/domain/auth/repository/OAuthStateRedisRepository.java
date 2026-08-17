package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/** OAuth CSRF 방지용 state 저장소 */
@Repository
@RequiredArgsConstructor
public class OAuthStateRedisRepository {

    private static final String PREFIX = "oauth:state:";
    private static final long STATE_TTL_SECONDS = 600; // 10분

    private final StringRedisTemplate redisTemplate;

    /** state 저장 */
    public void save(String state) {
        redisTemplate.opsForValue().set(PREFIX + state, "1", STATE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** state 검증 후 즉시 삭제 (일회용) */
    public boolean consumeState(String state) {
        Boolean deleted = redisTemplate.delete(PREFIX + state);
        return Boolean.TRUE.equals(deleted);
    }
}
