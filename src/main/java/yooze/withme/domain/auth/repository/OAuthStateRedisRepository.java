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

    /**
     * state 와 그에 대응하는 redirect_uri 를 함께 저장한다.
     * 콜백에서 카카오 토큰 요청 시 같은 redirect_uri 를 써야 하므로 state 에 묶어 보관한다.
     */
    public void save(String state, String redirectUri) {
        redisTemplate.opsForValue().set(PREFIX + state, redirectUri, STATE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * state 를 검증하고 즉시 삭제(일회용)한 뒤 저장된 redirect_uri 를 반환한다.
     * state 가 없거나 만료된 경우 null 을 반환한다.
     */
    public String consumeState(String state) {
        String redirectUri = redisTemplate.opsForValue().get(PREFIX + state);
        if (redirectUri == null) {
            return null;
        }
        redisTemplate.delete(PREFIX + state);
        return redirectUri;
    }
}
