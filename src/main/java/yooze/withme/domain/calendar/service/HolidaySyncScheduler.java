package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 공휴일 동기화 트리거. 실제 동기화와 중복 실행 방지는 HolidaySyncService 가 맡는다. */
@Component
@RequiredArgsConstructor
public class HolidaySyncScheduler {

    private final HolidaySyncService holidaySyncService;

    /** 매년 12월 1일 04:00(KST)에 다음 해분을 받아둔다. */
    @Scheduled(cron = "0 0 4 1 12 *", zone = "Asia/Seoul")
    public void syncNextYear() {
        holidaySyncService.sync(LocalDate.now().getYear() + 1);
    }

    /** 기동 보정 — 올해·내년 데이터가 비어 있을 때만 채운다(새 환경/초기 배포용). */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        int year = LocalDate.now().getYear();
        holidaySyncService.syncIfEmpty(year);
        holidaySyncService.syncIfEmpty(year + 1);
    }
}
