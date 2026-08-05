package yooze.withme.common.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import yooze.withme.common.properties.DiscordWebhookProperties;
import yooze.withme.common.status.ErrorStatus;

class DiscordErrorNotifierTest {

    private static DiscordErrorNotifier notifier(Duration cooldown) {
        return new DiscordErrorNotifier(
                new DiscordWebhookProperties("https://discord.test/webhook", true, cooldown),
                RestClient.create(),
                Runnable::run);
    }

    @Test
    @DisplayName("같은 에러가 쿨다운 안에 반복되면 첫 건만 전송하고 나머지는 억제 건수로 수집")
    void suppressesDuplicatesWithinCooldown() throws Exception {
        DiscordErrorNotifier notifier = notifier(Duration.ofMillis(50));
        String key = "java.lang.IllegalStateException#boom";

        assertThat(notifier.claimSend(key)).isZero();   // 첫 건은 전송
        assertThat(notifier.claimSend(key)).isNegative(); // 억제
        assertThat(notifier.claimSend(key)).isNegative();

        Thread.sleep(60);
        assertThat(notifier.claimSend(key)).isEqualTo(2); // 억제된 2건을 보고
        assertThat(notifier.claimSend(key)).isNegative();
    }

    @Test
    @DisplayName("스택트레이스가 Discord field 한계(1024자) 안으로 자름")
    void truncatesStackTrace() {
        assertThat(DiscordErrorNotifier.truncate("x".repeat(2000), 1000)).hasSize(1000);
    }

    @Test
    @DisplayName("억제된 건수가 있으면 본문에 표시")
    void includesSuppressedCountInBody() {
        Map<String, Object> payload = notifier(Duration.ofMinutes(5))
                .buildPayload(ErrorStatus.INTERNAL_SERVER_ERROR, new IllegalStateException("boom"), "GET /a", 7);

        assertThat(embed(payload).get("description").toString()).contains("7건");
    }

    @Test
    @DisplayName("url 이 비면 알림을 시도하지 않는다 (예외도 던지지 않는다)")
    void doesNothingWhenUrlIsBlank() {
        DiscordErrorNotifier notifier = new DiscordErrorNotifier(
                new DiscordWebhookProperties("", true, Duration.ofMinutes(5)),
                RestClient.create(),
                Runnable::run);

        Exception boom = new IllegalStateException("boom");
        notifier.notify(ErrorStatus.INTERNAL_SERVER_ERROR, boom);

        // 전송 시도 자체가 없었으므로 해당 키의 쿨다운도 소비되지 않았다.
        String key = boom.getClass().getName() + "#" + boom.getStackTrace()[0];
        assertThat(notifier.claimSend(key)).isZero();
    }

    @Test
    @DisplayName("Discord 전송 작업은 주입된 전용 executor에 제출")
    void usesInjectedExecutor() {
        AtomicBoolean submitted = new AtomicBoolean();
        DiscordErrorNotifier notifier = new DiscordErrorNotifier(
                new DiscordWebhookProperties("https://discord.test/webhook", true, Duration.ofMinutes(5)),
                RestClient.create(),
                task -> submitted.set(true));

        notifier.notify(ErrorStatus.INTERNAL_SERVER_ERROR, new IllegalStateException("boom"));

        assertThat(submitted).isTrue();
    }

    @Test
    @DisplayName("executor 가 작업을 거부하면 쿨다운을 소비하지 않는다 (같은 에러가 다시 전송 후보)")
    void doesNotConsumeCooldownWhenExecutorRejects() {
        DiscordErrorNotifier notifier = new DiscordErrorNotifier(
                new DiscordWebhookProperties("https://discord.test/webhook", true, Duration.ofMinutes(5)),
                RestClient.create(),
                task -> {
                    throw new RejectedExecutionException("queue full");
                });

        Exception boom = new IllegalStateException("boom");
        notifier.notify(ErrorStatus.INTERNAL_SERVER_ERROR, boom);

        // 전송이 시작되지 못했으므로 다음 같은 에러는 여전히 전송 후보여야 함.
        String key = boom.getClass().getName() + "#" + boom.getStackTrace()[0];
        assertThat(notifier.claimSend(key)).isZero();
    }

    @Test
    @DisplayName("거부된 전송이 들고 있던 억제 건수는 다음 전송이 보고함")
    void carriesSuppressedCountOverRejectedSend() {
        DiscordErrorNotifier notifier = notifier(Duration.ofMinutes(5));
        String key = "java.lang.IllegalStateException#boom";

        assertThat(notifier.claimSend(key)).isZero();
        notifier.releaseSend(key, 3);

        assertThat(notifier.claimSend(key)).isEqualTo(3);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> embed(Map<String, Object> payload) {
        return ((List<Map<String, Object>>) payload.get("embeds")).get(0);
    }
}
