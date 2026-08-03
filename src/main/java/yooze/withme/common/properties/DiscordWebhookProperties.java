package yooze.withme.common.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 5xx 에러 Discord 알림 설정.
 * url 은 그 자체가 인증 수단이므로 환경변수로만 주입
 */
@ConfigurationProperties(prefix = "discord.webhook")
public record DiscordWebhookProperties(
        String url,
        boolean enabled,
        Duration cooldown
) {
    public DiscordWebhookProperties {
        if (cooldown == null) {
            cooldown = Duration.ofMinutes(5);
        }
    }

    /** url 이 비면 알림만 off
     *  알림 설정 때문에 서버가 못 뜨는 일은 발생 X. */
    public boolean isUsable() {
        return enabled && url != null && !url.isBlank();
    }
}
