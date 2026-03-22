package com.authservice.config;

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

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // ─── API Info ─────────────────────────────
                .info(new Info()
                        .title("Auth Service API")
                        .version("1.0.0")
                        .description(
                            "Production-style Authentication Microservice\n\n" +
                            "## Features\n" +
                            "- JWT Authentication\n" +
                            "- Redis Token Storage\n" +
                            "- Rate Limiting\n" +
                            "- Account Lockout\n" +
                            "- Role Based Access Control\n" +
                            "- Email Verification\n" +
                            "- Password Reset via Email"
                        )
                        .contact(new Contact()
                                .name("Auth Service")
                                .email("admin@authservice.com"))
                        .license(new License()
                                .name("MIT License"))
                )
                // ─── JWT Security Scheme ──────────────────
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes(
                            "Bearer Authentication",
                            new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description(
                                    "Enter your JWT token here. " +
                                    "Get it from /api/auth/login"
                                )
                        )
                );
    }
}