package yooze.withme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import yooze.withme.common.properties.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class WithMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(WithMeApplication.class, args);
    }
}
