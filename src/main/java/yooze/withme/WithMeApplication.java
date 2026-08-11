package yooze.withme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import yooze.withme.common.properties.JwtProperties;
import yooze.withme.common.properties.KakaoProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, KakaoProperties.class})
public class WithMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WithMeApplication.class, args);
    }
}
