
package com.sportradar.offer_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI offerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sportradar Offer Service API")
                        .description("REST API for managing sports events, markets and outcomes")
                        .version("1.0.0"));
    }
}
