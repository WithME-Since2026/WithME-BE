package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class VerificationCodeRedisRepository {

    private static final String FIND_ID_PREFIX = "find-id:verify:";
    private static final String FIND_PW_PREFIX = "find-pw:verify:";
    private static final long CODE_TTL_SECONDS = 300; // 5분

    private final StringRedisTemplate redisTemplate;

    // ── 아이디 찾기 ──────────────────────────────────────────────

    public void save(String email, String code) {
        set(FIND_ID_PREFIX + email, code);
    }

    public Optional<String> findByEmail(String email) {
        return get(FIND_ID_PREFIX + email);
    }

    public void deleteByEmail(String email) {
        redisTemplate.delete(FIND_ID_PREFIX + email);
    }

    // ── 비밀번호 찾기 ─────────────────────────────────────────────

    public void saveForPasswordReset(String email, String code) {
        set(FIND_PW_PREFIX + email, code);
    }

    public Optional<String> findForPasswordReset(String email) {
        return get(FIND_PW_PREFIX + email);
    }

    public void deleteForPasswordReset(String email) {
        redisTemplate.delete(FIND_PW_PREFIX + email);
    }

    // ── 공통 ─────────────────────────────────────────────────────

    private void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value, CODE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
}
