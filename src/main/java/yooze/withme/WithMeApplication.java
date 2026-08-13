package yooze.withme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import yooze.withme.common.properties.DiscordWebhookProperties;
import yooze.withme.common.properties.JwtProperties;
import yooze.withme.common.properties.KakaoProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, DiscordWebhookProperties.class, KakaoProperties.class})
public class WithMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WithMeApplication.class, args);
    }
}
