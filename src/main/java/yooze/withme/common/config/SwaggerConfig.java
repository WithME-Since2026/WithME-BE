package yooze.withme.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // Swagger Authorize 버튼 누르면 모든 요청에 Bearer 헤더 자동 포함
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 기반 bearer 인증")
                        )
                )
                .info(new Info()
                        .title("WithME API")
                        .description("WithME 백엔드 API 명세")
                        .version("v1")
                        .contact(
                                new Contact()
                                        .name("WithME Dev Team")
                                        .email("withme2026official@gmail.com")
                        )
                        .license(new License()
                                .name("Apache License 2.0 with Commons Clause")
                                .url("https://commonsclause.com/")));
    }
}
