package com.movie.booking.catalog_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearer-jwt";

    // The global requirement makes Swagger UI send the token entered in Authorize with every call.
    @Bean
    OpenAPI openApi(@Value("${spring.application.name}") String applicationName) {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName)
                        .version("v1")
                        .description("Movies, theaters, screens and shows."))
                .components(new Components().addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }

    // Every operation needs a token (401); writes also need the admin role (403). Declared once here, not per method.
    @Bean
    OperationCustomizer securityResponses() {
        return (operation, handlerMethod) -> {
            addResponse(operation, "401", "Missing or invalid bearer token");
            if (isWrite(handlerMethod)) {
                addResponse(operation, "403", "Authenticated, but the token lacks the catalog admin role");
            }
            return operation;
        };
    }

    private static boolean isWrite(HandlerMethod handlerMethod) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class);
        return mapping != null && Arrays.stream(mapping.method()).anyMatch(m -> m != RequestMethod.GET);
    }

    private static void addResponse(Operation operation, String code, String description) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        operation.getResponses().addApiResponse(code, new ApiResponse().description(description));
    }
}
