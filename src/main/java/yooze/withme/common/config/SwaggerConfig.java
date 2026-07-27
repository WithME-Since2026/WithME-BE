package yooze.withme.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // 각 컨트롤러의 @SecurityRequirement(name = ...) 과 반드시 동일해야 한다.
    // OpenAPI 의 securitySchemes 키는 ^[a-zA-Z0-9._-]+$ 만 허용하므로 공백을 쓸 수 없다.
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("WithME API")
                .description("WithME 백엔드 API 명세")
                .version("v1")
                .contact(new Contact()
                        .name("WithME Dev Team")
                        .email("withme2026official@gmail.com"))
                .license(new License()
                        .name("Apache License 2.0 with Commons Clause")
                        // Commons Clause 안내 페이지가 아니라 Apache 2.0 본문까지 포함한
                        // 저장소의 라이선스 전문을 가리킨다.
                        .url("https://github.com/WithME-Since2026/WithME-BE/blob/develop/LICENSE.txt"));

        // JWT 토큰 헤더 방식
        // type=HTTP 에서는 name 이 적용되지 않는다(apiKey 전용)므로 지정하지 않는다.
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 기반 bearer 인증"));

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/"))
                // 전역 security 미적용 → 각 엔드포인트에서 @SecurityRequirement로 개별 지정
                .components(components);
    }
}
