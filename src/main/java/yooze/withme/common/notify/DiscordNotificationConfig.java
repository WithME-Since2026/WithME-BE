package yooze.withme.common.notify;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class DiscordNotificationConfig {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    private static final int EXECUTOR_THREADS = 2;
    private static final int QUEUE_CAPACITY = 100;

    @Bean("discordRestClient")
    RestClient discordRestClient() {
        return createRestClient();
    }

    @Bean(name = "discordNotificationExecutor", destroyMethod = "shutdown")
    ExecutorService discordNotificationExecutor() {
        return new ThreadPoolExecutor(
                EXECUTOR_THREADS,
                EXECUTOR_THREADS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                Thread.ofPlatform().name("discord-notifier-", 0).daemon(true).factory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    static RestClient createRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(RESPONSE_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
