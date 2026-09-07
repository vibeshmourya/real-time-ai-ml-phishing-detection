package com.phishshield.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, createSecurityScheme()))
                .info(new Info()
                        .title("PhishShield Cybersecurity Platform API")
                        .description("Real-Time AI/ML-Based Phishing Detection, Explainability, and Cyber Prevention Platform REST API.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PhishShield Cyber Response Team")
                                .email("security@phishshield.internal"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
    }

    private SecurityScheme createSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your valid JWT Bearer token to authorize requests");
    }
}
