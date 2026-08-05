package yooze.withme.common.notify;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import yooze.withme.common.base.BaseStatus;
import yooze.withme.common.properties.DiscordWebhookProperties;

/**
 * 5xx 응답을 Discord 웹훅으로 알림
 * 알림 전송이 실패해도 절대 호출자에게 예외를 던지지 않음
 * 알림은 로그의 사본일 뿐이고
 * 원본은 어차피 log.error 로 남음.
 */
@Slf4j
@Component
public class DiscordErrorNotifier {

    // Discord embed field value 한계가 1024자. 코드블록 fence 자리를 남겨둠
    private static final int STACK_TRACE_LIMIT = 1000;
    private static final int RED = 15158332;
    private static final DateTimeFormatter KST =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    private final DiscordWebhookProperties properties;
    private final RestClient restClient;
    private final Executor executor;

    // 인스턴스 로컬 쿨다운. 다중 인스턴스로 늘리면 Redis로 옮김.
    // 키는 예외 종류 단위라 코드 크기에 비례해 상한이 있으므로 만료 정리는 두지 않음.
    private final Map<String, Cooldown> cooldowns = new ConcurrentHashMap<>();

    public DiscordErrorNotifier(
            DiscordWebhookProperties properties,
            @Qualifier("discordRestClient") RestClient restClient,
            @Qualifier("discordNotificationExecutor") Executor executor) {
        this.properties = properties;
        this.restClient = restClient;
        this.executor = executor;
    }

    public void notify(BaseStatus status, Throwable e) {
        if (!properties.isUsable()) {
            return;
        }
        try {
            int suppressed = claimSend(cooldownKey(e));
            if (suppressed < 0) {
                return;
            }
            // 요청 정보는 요청 스레드에서만 읽을 수 있으므로 비동기 전송 전에 문자열로 뽑아둠
            Map<String, Object> payload = buildPayload(status, e, currentRequest(), suppressed);
            CompletableFuture.runAsync(() -> send(payload), executor);
        } catch (Exception notifyFailure) {
            log.warn("[*] Discord 알림 준비 실패 : {}", notifyFailure.getMessage());
        }
    }

    private void send(Map<String, Object> payload) {
        try {
            restClient.post()
                    .uri(properties.url())
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("[*] Discord 알림 전송 실패 : {}", e.getMessage());
        }
    }

    /**
     * 쿨다운을 통과하면 그동안 억제된 건수를, 쿨다운 중이면 -1 을 반환
     * 5xx 는 장애 때 초당 수백 건이 될 수 있고 Discord 웹훅은 분당 30건 남짓이라 반드시 필요
     */
    int claimSend(String key) {
        Cooldown cooldown = cooldowns.computeIfAbsent(key, k -> new Cooldown());
        long now = System.currentTimeMillis();
        synchronized (cooldown) {
            if (now - cooldown.lastSentAt < properties.cooldown().toMillis()) {
                cooldown.suppressed++;
                return -1;
            }
            cooldown.lastSentAt = now;
            int suppressed = cooldown.suppressed;
            cooldown.suppressed = 0;
            return suppressed;
        }
    }

    Map<String, Object> buildPayload(BaseStatus status, Throwable e, String request, int suppressed) {
        Instant occurredAt = Instant.now();

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("발생 시각", KST.format(occurredAt)));
        fields.add(field("상태 코드", status.getHttpStatus().value() + " / " + status.getCode()));
        fields.add(field("요청", request));
        fields.add(field("예외", e.getClass().getSimpleName() + ": " + e.getMessage()));
        fields.add(field("스택트레이스", "```" + truncate(stackTrace(e), STACK_TRACE_LIMIT) + "```"));

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "🚨 " + status.getHttpStatus().value() + " " + status.getHttpStatus().name());
        embed.put("color", RED);
        embed.put("timestamp", occurredAt.toString());
        embed.put("fields", fields);
        if (suppressed > 0) {
            embed.put("description", "직전 쿨다운 동안 같은 에러 " + suppressed + "건이 억제되었습니다.");
        }
        return Map.of("embeds", List.of(embed));
    }

    private static Map<String, Object> field(String name, String value) {
        return Map.of("name", name, "value", value, "inline", false);
    }

    private static String cooldownKey(Throwable e) {
        StackTraceElement[] trace = e.getStackTrace();
        return e.getClass().getName() + "#" + (trace.length > 0 ? trace[0].toString() : "");
    }

    private static String stackTrace(Throwable e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        String suffix = "\n... (생략)";
        return value.substring(0, max - suffix.length()) + suffix;
    }

    private static String currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "(요청 정보 없음)";
    }

    /** 접근은 전부 synchronized(cooldown) 안에서만 발생 */
    private static final class Cooldown {
        private long lastSentAt;
        private int suppressed;
    }
}
