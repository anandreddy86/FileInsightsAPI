package com.fileinsights.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // This applies to all /api endpoints
                .allowedOrigins("http://localhost:3000")  // Frontend URL (React)
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");  // Allow all headers
    }
}
