package yooze.withme.common.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import yooze.withme.common.properties.DiscordWebhookProperties;
import yooze.withme.common.status.ErrorStatus;

/** 실제 Discord 로 메시지를 보내는 수동 확인용. DISCORD_WEBHOOK_URL 이 있을 때만 실행된다. */
@EnabledIfEnvironmentVariable(named = "DISCORD_WEBHOOK_URL", matches = ".+")
class DiscordWebhookSmokeTest {

    @Test
    void 실제_웹훅으로_전송된다() {
        String url = System.getenv("DISCORD_WEBHOOK_URL");
        Map<String, Object> payload = new DiscordErrorNotifier(
                new DiscordWebhookProperties(url, true, Duration.ofMinutes(5)))
                .buildPayload(
                        ErrorStatus.INTERNAL_SERVER_ERROR,
                        new IllegalStateException("smoke test"),
                        "GET /smoke-test",
                        3);

        ResponseEntity<Void> response = RestClient.create().post()
                .uri(url).body(payload).retrieve().toBodilessEntity();

        System.out.println("[SMOKE] Discord 응답: " + response.getStatusCode());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
