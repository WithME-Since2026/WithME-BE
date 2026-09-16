package yooze.withme.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

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
                // 운영: 환경변수로 절대경로(/app/firebase/...)를 받아 FileSystemResource로 읽음
                // 로컬: 환경변수 미설정 시 기본값(firebase/service-account-key.json)으로
                //       ClassPathResource(src/main/resources/)에서 읽음
                org.springframework.core.io.Resource keyResource =
                        new FileSystemResource(serviceAccountKeyPath).exists()
                                ? new FileSystemResource(serviceAccountKeyPath)
                                : new ClassPathResource(serviceAccountKeyPath);
                InputStream serviceAccount = keyResource.getInputStream();

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
