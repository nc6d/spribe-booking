package org.spribe.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI bookingSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking System API")
                        .description("REST API for managing accommodation bookings")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Spribe-Test")
                                .email("example@spribe.com")));
    }
}