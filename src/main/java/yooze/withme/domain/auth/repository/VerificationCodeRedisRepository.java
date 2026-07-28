package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class VerificationCodeRedisRepository {

    private static final String KEY_PREFIX = "find-id:verify:";
    private static final long CODE_TTL_SECONDS = 300; // 5분

    private final StringRedisTemplate redisTemplate;

    /** 인증코드 저장 */
    public void save(String email, String code) {
        redisTemplate.opsForValue().set(KEY_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 인증코드 조회 */
    public Optional<String> findByEmail(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + email));
    }

    /** 인증코드 삭제 (검증 완료 후 재사용 방지) */
    public void deleteByEmail(String email) {
        redisTemplate.delete(KEY_PREFIX + email);
    }
}
