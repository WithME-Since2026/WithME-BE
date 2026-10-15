package yooze.withme.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Spring Security 기본 entry point 는 인증 실패에도 403 을 준다.
 * 인증 실패(401)와 권한 부족(403)이 다시 섞이지 않도록 고정해 둔다.
 */
@SpringBootTest(properties = {"management.health.redis.enabled=false", "management.health.mail.enabled=false"})
@AutoConfigureMockMvc
class AuthenticationEntryPointTest {

    // 컨트롤러 존재 여부와 무관하게 anyRequest().authenticated() 에 걸리는 경로
    private static final String PROTECTED = "/api/v1/entry-point-test";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("토큰 없이 요청하면 401")
    void noToken() throws Exception {
        assertThat(status(MockMvcRequestBuilders.get(PROTECTED)))
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("잘못된 토큰으로 요청하면 401")
    void invalidToken() throws Exception {
        assertThat(status(MockMvcRequestBuilders.get(PROTECTED).header("Authorization", "Bearer invalid.token.value")))
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("인증됐지만 ADMIN 이 아니면 admin 경로는 403")
    void notAdmin() throws Exception {
        assertThat(status(MockMvcRequestBuilders.get("/api/v1/admin/entry-point-test").with(user("user").roles("USER"))))
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private int status(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }
}
