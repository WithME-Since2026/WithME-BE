package yooze.withme.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공공데이터포털 특일 정보 설정.
 * 인증키는 그 자체가 자격증명이므로 환경변수로만 주입한다.
 */
@ConfigurationProperties(prefix = "holiday")
public record HolidayProperties(
        String serviceKey,
        boolean enabled
) {

    /** 키가 없으면 동기화만 건너뛴다. 설정 때문에 서버가 못 뜨는 일은 없어야 한다. */
    public boolean isUsable() {
        return enabled && serviceKey != null && !serviceKey.isBlank();
    }
}
