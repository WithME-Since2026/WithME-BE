package yooze.withme.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@org.springframework.context.annotation.Profile("!test")
public class FirebaseConfig {

    @Value("${firebase.service-account-key-path}")
    private String serviceAccountKeyPath;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount =
                        new ClassPathResource(serviceAccountKeyPath).getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("[*] Firebase 초기화 완료");
            } catch (IOException e) {
                log.error("[*] Firebase 초기화 실패: {}", e.getMessage());
                throw new IllegalStateException("Firebase 초기화에 실패했습니다.", e);
            }
        }
    }
}
