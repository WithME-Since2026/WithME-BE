package yooze.withme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import yooze.withme.common.properties.DiscordWebhookProperties;
import yooze.withme.common.properties.HolidayProperties;
import yooze.withme.common.properties.JwtProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        DiscordWebhookProperties.class,
        HolidayProperties.class
})
public class WithMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WithMeApplication.class, args);
    }
}
