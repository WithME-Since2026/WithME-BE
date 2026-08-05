package yooze.withme.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * /actuator/health 는 배포 헬스체크가 쓰므로 열려 있어야 하고,
 * /actuator/prometheus 는 지표가 새므로 닫혀 있어야 한다. 둘 다 조용히 깨지는 종류라 고정해 둔다.
 * (테스트 프로필 application.yml 이 main 을 가리므로 노출 설정은 여기서 직접 준다)
 */
@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,prometheus",
        // 테스트에는 Redis 가 없어 health 가 503 이 된다. 여기서 볼 것은 인증 여부지 Redis 상태가 아니다.
        "management.health.redis.enabled=false"})
@AutoConfigureMockMvc
class ActuatorEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("health 는 인증 없이 열리고, prometheus 는 인증 없이는 막힌다")
    void actuator_노출_범위() throws Exception {
        int health = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertThat(health).isEqualTo(HttpStatus.OK.value());

        int prometheus = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus"))
                .andReturn().getResponse().getStatus();
        assertThat(prometheus)
                .isIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
    }
}
