package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class VerificationCodeRedisRepository {

    private static final String FIND_ID_PREFIX = "find-id:verify:";
    private static final String FIND_PW_PREFIX = "find-pw:verify:";
    private static final long CODE_TTL_SECONDS = 300; // 5분

    /**
     * 값 일치 시 원자적으로 삭제하는 Lua 스크립트
     * 반환값: 1 = 일치 후 삭제 성공, 0 = 불일치 또는 키 없음
     */
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local val = redis.call('GET', KEYS[1]) " +
            "if val == ARGV[1] then " +
            "  redis.call('DEL', KEYS[1]) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    // ── 아이디 찾기 ──────────────────────────────────────────────

    public void save(String email, String code) {
        redisTemplate.opsForValue().set(FIND_ID_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 코드 일치 시 원자적으로 삭제 후 true 반환, 불일치/만료 시 false */
    public boolean consumeByEmail(String email, String code) {
        Long result = redisTemplate.execute(CONSUME_SCRIPT, List.of(FIND_ID_PREFIX + email), code);
        return Long.valueOf(1L).equals(result);
    }

    public void deleteByEmail(String email) {
        redisTemplate.delete(FIND_ID_PREFIX + email);
    }

    // ── 비밀번호 찾기 ─────────────────────────────────────────────

    public void saveForPasswordReset(String email, String code) {
        redisTemplate.opsForValue().set(FIND_PW_PREFIX + email, code, CODE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /** 코드 일치 시 원자적으로 삭제 후 true 반환, 불일치/만료 시 false */
    public boolean consumeForPasswordReset(String email, String code) {
        Long result = redisTemplate.execute(CONSUME_SCRIPT, List.of(FIND_PW_PREFIX + email), code);
        return Long.valueOf(1L).equals(result);
    }

    public void deleteForPasswordReset(String email) {
        redisTemplate.delete(FIND_PW_PREFIX + email);
    }
}
