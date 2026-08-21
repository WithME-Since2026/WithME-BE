package yooze.withme.domain.calendar.service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import yooze.withme.common.notify.DiscordErrorNotifier;
import yooze.withme.common.properties.HolidayProperties;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.enums.HolidayType;
import yooze.withme.domain.calendar.repository.HolidayRepository;

/**
 * 특일 정보를 연 단위로 받아 holidays 테이블에 캐싱한다.
 * 실패해도 캘린더는 이미 캐싱된 DB를 그대로 서빙하므로 예외를 밖으로 던지지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HolidaySyncService {

    private static final String LOCK_PREFIX = "holiday:sync:";
    /** 락은 성공해도 풀지 않는다. TTL 동안은 다른 인스턴스가 같은 해를 다시 부르지 않게 하는 게 목적. */
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private final HolidayApiClient holidayApiClient;
    private final HolidayRepository holidayRepository;
    private final HolidayProperties holidayProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final DiscordErrorNotifier discordErrorNotifier;

    /**
     * 해당 연도의 특일을 동기화한다.
     * @return 실제로 동기화를 수행했으면 true, 키가 없거나 다른 인스턴스가 이미 잡고 있으면 false
     */
    public boolean sync(int year) {
        if (!holidayProperties.isUsable()) {
            log.info("[*] 공휴일 동기화 건너뜀 (인증키 미설정 또는 비활성)");
            return false;
        }
        // Blue-Green 전환 중에는 두 컨테이너가 동시에 떠 있으므로 한쪽만 호출하게 막는다
        if (!Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + year, "1", LOCK_TTL))) {
            log.info("[*] 공휴일 동기화 건너뜀 (다른 인스턴스가 {}년 처리 중)", year);
            return false;
        }

        try {
            for (HolidayType type : HolidayType.values()) {
                upsertAll(holidayApiClient.fetch(type, year));
            }
            log.info("[*] 공휴일 동기화 완료 : {}년", year);
            return true;
        } catch (Exception e) {
            log.warn("[*] 공휴일 동기화 실패 : {}년 - {}", year, e.getMessage());
            discordErrorNotifier.notify(ErrorStatus.HOLIDAY_SYNC_FAILED, e);
            return false;
        }
    }

    /** 올해·내년 데이터가 하나도 없으면 채운다. 기동 보정용. */
    public void syncIfEmpty(int year) {
        if (!holidayProperties.isUsable()) {
            return;
        }
        if (!holidayRepository.existsByDateBetween(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        )) {
            sync(year);
        }
    }

    /**
     * (date, name) 기준 upsert — 재실행해도 중복이 쌓이지 않는다.
     * 외부 API 호출을 트랜잭션 안에 넣지 않으려고 더티체킹 대신 save 로 저장한다.
     */
    private void upsertAll(List<Holiday> holidays) {
        for (Holiday holiday : holidays) {
            Holiday target = holidayRepository
                    .findByDateAndName(holiday.getDate(), holiday.getName())
                    .orElse(holiday);
            target.update(holiday.getType(), holiday.isRestDay());
            holidayRepository.save(target);
        }
    }
}
