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

    // 각 컨트롤러의 @SecurityRequirement(name = "JWT TOKEN") 과 반드시 동일해야 한다.
    private static final String SECURITY_SCHEME_NAME = "JWT TOKEN";

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
                        .url("https://commonsclause.com/"));

        // JWT 토큰 헤더 방식
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Bearer")
                        .bearerFormat("JWT")
                        .description("JWT 기반 bearer 인증"));

        return new OpenAPI()
                .info(info)
                .addServersItem(new Server().url("/"))
                // 전역 security 미적용 → 각 엔드포인트에서 @SecurityRequirement로 개별 지정
                .components(components);
    }
}
