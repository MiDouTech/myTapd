package com.miduo.cloud.ticket.controller.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("内部工单系统 API")
                        .description("米多内部工单系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("米多团队")
                                .url("https://www.miduo.com")));
    }
}
