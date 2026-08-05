package yooze.withme.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RateLimitRedisRepository {

    private static final String SEND_LIMIT_PREFIX = "rate:send:";
    private static final String VERIFY_FAIL_PREFIX = "rate:verify-fail:";

    /** 이메일당 재발송 대기 시간 (초) */
    private static final long SEND_COOLDOWN_SECONDS = 60;

    /** 인증코드 검증 최대 실패 허용 횟수 */
    private static final long MAX_VERIFY_FAIL_COUNT = 5;

    private final StringRedisTemplate redisTemplate;

    /**
     * 발송 제한 확인 및 설정
     * @return true = 발송 가능, false = 쿨다운 중 (재발송 불가)
     */
    public boolean checkAndSetSendLimit(String email) {
        String key = SEND_LIMIT_PREFIX + email;
        Boolean set = redisTemplate.opsForValue().setIfAbsent(key, "1", SEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(set);
    }

    /**
     * 검증 실패 횟수 증가
     * @return 현재 누적 실패 횟수
     */
    public long incrementVerifyFail(String email, long ttlSeconds) {
        String key = VERIFY_FAIL_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 첫 실패 시 TTL 설정 (코드 만료 시간과 동일)
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        }
        return count != null ? count : 1;
    }

    /** 검증 성공 시 실패 카운터 초기화 */
    public void deleteVerifyFail(String email) {
        redisTemplate.delete(VERIFY_FAIL_PREFIX + email);
    }

    /** 최대 실패 횟수 반환 */
    public long getMaxVerifyFailCount() {
        return MAX_VERIFY_FAIL_COUNT;
    }

}
